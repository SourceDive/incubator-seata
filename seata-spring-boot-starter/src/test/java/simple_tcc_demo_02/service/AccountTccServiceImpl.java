package simple_tcc_demo_02.service;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 账户 TCC 服务实现类<br><br>
 * 
 * 使用数据库存储 TCC 状态，实现真正的分布式事务<br>
 * 通过 tcc_record 表记录 Try-Confirm-Cancel 的状态
 */
@Service
public class AccountTccServiceImpl implements AccountTccService {

    private final JdbcTemplate jdbcTemplate;

    public AccountTccServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryDeduct(String accountId, int amount) {
        System.out.println("===> Try 阶段：冻结账户金额 ===");
        System.out.println("账户ID: " + accountId + ", 金额: " + amount);
        
        try {
            // 1. 检查账户余额是否足够
            Integer currentBalance = jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE id = ?", 
                Integer.class, 
                accountId
            );
            
            if (currentBalance == null) {
                System.out.println("❌ 账户不存在: " + accountId);
                return false;
            }
            
            if (currentBalance < amount) {
                System.out.println("❌ 余额不足，当前余额: " + currentBalance + ", 需要: " + amount);
                return false;
            }
            
            // 2. 生成 TCC 记录ID
            String tccId = UUID.randomUUID().toString();
            String xid = getCurrentXid();
            long branchId = System.currentTimeMillis();
            
            // 3. 在数据库中记录 TCC 状态
            int inserted = jdbcTemplate.update(
                "INSERT INTO tcc_record (id, xid, branch_id, account_id, amount, status) VALUES (?, ?, ?, ?, ?, 'TRY')",
                tccId, xid, branchId, accountId, amount
            );
            
            if (inserted == 0) {
                System.out.println("❌ TCC 记录插入失败");
                return false;
            }
            
            System.out.println("✅ 金额冻结成功，TCC记录ID: " + tccId + ", 冻结金额: " + amount);
            
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Try 阶段失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean confirm(BusinessActionContext context) {
        System.out.println("===> Confirm 阶段：真正扣款 ===");
        
        try {
            // 从上下文中获取参数
            String accountId = (String) context.getActionContext("accountId");
            Integer amount = (Integer) context.getActionContext("amount");
            String xid = context.getXid();
            
            System.out.println("账户ID: " + accountId + ", 扣款金额: " + amount + ", XID: " + xid);
            
            // 1. 查找对应的 TCC 记录
            Integer recordAmount = jdbcTemplate.queryForObject(
                "SELECT amount FROM tcc_record WHERE xid = ? AND account_id = ? AND status = 'TRY'",
                Integer.class, xid, accountId
            );
            
            if (recordAmount == null || !recordAmount.equals(amount)) {
                System.out.println("❌ 未找到对应的 TCC 记录或金额不匹配");
                return false;
            }
            
            // 2. 真正扣款
            int updatedRows = jdbcTemplate.update(
                "UPDATE account SET balance = balance - ? WHERE id = ?", 
                amount, 
                accountId
            );
            
            if (updatedRows == 0) {
                System.out.println("❌ 扣款失败");
                return false;
            }
            
            // 3. 更新 TCC 记录状态为 CONFIRM
            jdbcTemplate.update(
                "UPDATE tcc_record SET status = 'CONFIRM' WHERE xid = ? AND account_id = ? AND status = 'TRY'",
                xid, accountId
            );
            
            System.out.println("✅ 扣款成功，实际扣款: " + amount);
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Confirm 阶段失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean cancel(BusinessActionContext context) {
        System.out.println("===> Cancel 阶段：解冻金额 ===");
        
        try {
            // 从上下文中获取参数
            String inputAccountId = (String) context.getActionContext("accountId");
            Integer inputAmount = (Integer) context.getActionContext("amount");
            String xid = context.getXid();
            
            System.out.println("账户ID: " + inputAccountId + ", 解冻金额: " + inputAmount + ", XID: " + xid);
            
            // 1. 查找对应的 TCC 记录
            Integer tccRecordAmount = jdbcTemplate.queryForObject(
                "SELECT amount FROM tcc_record WHERE xid = ? AND account_id = ? AND status = 'TRY'",
                Integer.class, xid, inputAccountId
            );

            if (inputAmount == null || tccRecordAmount.intValue() !=  inputAmount.intValue()) {
                System.out.println("❌ 未找到对应的 TCC 记录或金额不匹配");
                return false;
            }

            // 2. 更新 TCC 记录状态为 CANCEL（解冻）
            int updated = updateStatusToCancel(xid, inputAccountId);
            if (updated == 0) {
                System.out.println("❌ TCC 记录状态更新失败");
                return false;
            }
            
            System.out.println("✅ 金额解冻成功，解冻金额: " + inputAmount);
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Cancel 阶段失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private int updateStatusToCancel(String xid, String accountId) {
        int updated = jdbcTemplate.update(
            "UPDATE tcc_record SET status = 'CANCEL' WHERE xid = ? AND account_id = ? AND status = 'TRY'",
                xid, accountId
        );
        return updated;
    }

    /**
     * 获取当前冻结的金额（用于测试）<br>
     * 从数据库查询 TRY 状态的记录
     */
    public int getFrozenAmount(String accountId) {
        try {
            Integer amount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM tcc_record WHERE account_id = ? AND status = 'TRY'",
                Integer.class, accountId
            );
            return amount != null ? amount : 0;
        } catch (Exception e) {
            System.out.println("查询冻结金额失败: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 清理所有冻结记录（用于测试）<br>
     * 清理数据库中的 TCC 记录
     */
    public void clearFrozenAmounts() {
        try {
            jdbcTemplate.update("DELETE FROM tcc_record");
            System.out.println("✅ 已清理所有 TCC 记录");
        } catch (Exception e) {
            System.out.println("❌ 清理 TCC 记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前 XID<br>
     * 用于 TCC 记录关联
     */
    private String getCurrentXid() {
        String xid = RootContext.getXID();
        return xid != null ? xid : "local-" + System.currentTimeMillis();
    }
}

