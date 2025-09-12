package simple_tcc_demo_01.service;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的 TCC 服务 - 单数据库版本
 * 
 * 业务场景：账户转账
 * - Try: 冻结转出账户金额，检查转入账户
 * - Confirm: 真正执行转账
 * - Cancel: 解冻转出账户金额
 */
@LocalTCC
@Service
public class SimpleTccService {

    private final JdbcTemplate jdbcTemplate;
    
    // 存储冻结记录，实际项目中应该用数据库存储
    private final Map<String, TransferRecord> frozenTransfers = new ConcurrentHashMap<>();

    public SimpleTccService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Try 阶段：冻结转出账户金额，检查转入账户
     */
    @TwoPhaseBusinessAction(
        name = "simpleTransfer", 
        commitMethod = "confirmTransfer", 
        rollbackMethod = "cancelTransfer"
    )
    public boolean tryTransfer(String fromAccountId, String toAccountId, int amount) {
        System.out.println("===> Try 阶段：准备转账 ===");
        System.out.println("从账户: " + fromAccountId + " 到账户: " + toAccountId + " 金额: " + amount);
        
        try {
            // 1. 检查转出账户余额
            Integer fromBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE id = ?", 
                Integer.class, 
                fromAccountId
            );
            
            if (fromBalance == null) {
                System.out.println("❌ 转出账户不存在: " + fromAccountId);
                return false;
            }
            
            if (fromBalance < amount) {
                System.out.println("❌ 转出账户余额不足，当前余额: " + fromBalance + ", 需要: " + amount);
                return false;
            }
            
            // 2. 检查转入账户是否存在
            Integer toBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE id = ?", 
                Integer.class, 
                toAccountId
            );
            
            if (toBalance == null) {
                System.out.println("❌ 转入账户不存在: " + toAccountId);
                return false;
            }
            
            // 3. 记录转账信息（冻结）
            String transferId = fromAccountId + "_" + toAccountId + "_" + System.currentTimeMillis();
            TransferRecord record = new TransferRecord(transferId, fromAccountId, toAccountId, amount);
            frozenTransfers.put(transferId, record);
            
            System.out.println("✅ Try 阶段成功，转账ID: " + transferId);
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Try 阶段失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * Confirm 阶段：真正执行转账
     */
    public boolean confirmTransfer(BusinessActionContext context) {
        System.out.println("===> Confirm 阶段：执行转账 ===");
        
        try {
            // 从上下文中获取参数
            String fromAccountId = (String) context.getActionContext("fromAccountId");
            String toAccountId = (String) context.getActionContext("toAccountId");
            Integer amount = (Integer) context.getActionContext("amount");
            
            System.out.println("从账户: " + fromAccountId + " 到账户: " + toAccountId + " 金额: " + amount);
            
            // 查找冻结记录
            TransferRecord record = findFrozenRecord(fromAccountId, toAccountId, amount);
            if (record == null) {
                System.out.println("❌ 未找到对应的冻结记录");
                return false;
            }
            
            // 执行转账
            int fromUpdated = jdbcTemplate.update(
                "UPDATE account SET balance = balance - ? WHERE id = ?", 
                amount, 
                fromAccountId
            );
            
            int toUpdated = jdbcTemplate.update(
                "UPDATE account SET balance = balance + ? WHERE id = ?", 
                amount, 
                toAccountId
            );
            
            if (fromUpdated == 0 || toUpdated == 0) {
                System.out.println("❌ 转账执行失败");
                return false;
            }
            
            // 清理冻结记录
            frozenTransfers.remove(record.getTransferId());
            
            System.out.println("✅ 转账执行成功");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Confirm 阶段失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cancel 阶段：解冻转出账户金额
     */
    public boolean cancelTransfer(BusinessActionContext context) {
        System.out.println("===> Cancel 阶段：取消转账 ===");
        
        try {
            // 从上下文中获取参数
            String fromAccountId = (String) context.getActionContext("fromAccountId");
            String toAccountId = (String) context.getActionContext("toAccountId");
            Integer amount = (Integer) context.getActionContext("amount");
            
            System.out.println("从账户: " + fromAccountId + " 到账户: " + toAccountId + " 金额: " + amount);
            
            // 查找冻结记录
            TransferRecord record = findFrozenRecord(fromAccountId, toAccountId, amount);
            if (record == null) {
                System.out.println("❌ 未找到对应的冻结记录");
                return false;
            }
            
            // 清理冻结记录
            frozenTransfers.remove(record.getTransferId());
            
            System.out.println("✅ 转账取消成功");
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Cancel 阶段失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 查找冻结记录
     */
    private TransferRecord findFrozenRecord(String fromAccountId, String toAccountId, int amount) {
        return frozenTransfers.values().stream()
            .filter(record -> record.getFromAccountId().equals(fromAccountId) 
                && record.getToAccountId().equals(toAccountId) 
                && record.getAmount() == amount)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 获取冻结记录数量（用于测试）
     */
    public int getFrozenRecordCount() {
        return frozenTransfers.size();
    }
    
    /**
     * 清理所有冻结记录（用于测试）
     */
    public void clearFrozenRecords() {
        frozenTransfers.clear();
    }
    
    /**
     * 转账记录内部类
     */
    private static class TransferRecord {
        private final String transferId;
        private final String fromAccountId;
        private final String toAccountId;
        private final int amount;
        
        public TransferRecord(String transferId, String fromAccountId, String toAccountId, int amount) {
            this.transferId = transferId;
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.amount = amount;
        }
        
        public String getTransferId() { return transferId; }
        public String getFromAccountId() { return fromAccountId; }
        public String getToAccountId() { return toAccountId; }
        public int getAmount() { return amount; }
    }
}

