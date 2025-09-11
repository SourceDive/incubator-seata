package simple_tcc_demo_01.service;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账户 TCC 服务实现类
 * 
 * 演示最简单的 TCC 模式实现
 */
@Service
public class AccountTccServiceImpl implements AccountTccService {

    private final JdbcTemplate jdbcTemplate;
    
    // 用于存储 Try 阶段的数据，实际项目中应该用数据库存储
    private final Map<String, Integer> frozenAmounts = new ConcurrentHashMap<>();

    public AccountTccServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryDeduct(String accountId, int amount) {
        System.out.println("=== Try 阶段：冻结账户金额 ===");
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
            
            // 2. 冻结金额（在实际项目中，这里应该更新数据库）
            frozenAmounts.put(accountId, amount);
            
            // 3. 记录冻结日志（模拟）
            System.out.println("✅ 金额冻结成功，冻结金额: " + amount);
            
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Try 阶段失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean confirm(BusinessActionContext context) {
        System.out.println("=== Confirm 阶段：真正扣款 ===");
        
        try {
            // 从上下文中获取参数
            String accountId = (String) context.getActionContext("accountId");
            Integer amount = (Integer) context.getActionContext("amount");
            
            System.out.println("账户ID: " + accountId + ", 扣款金额: " + amount);
            
            // 1. 检查是否在 Try 阶段冻结了金额
            Integer frozenAmount = frozenAmounts.get(accountId);
            if (frozenAmount == null || !frozenAmount.equals(amount)) {
                System.out.println("❌ 未找到对应的冻结金额");
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
            
            // 3. 清理冻结记录
            frozenAmounts.remove(accountId);
            
            System.out.println("✅ 扣款成功，实际扣款: " + amount);
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Confirm 阶段失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean cancel(BusinessActionContext context) {
        System.out.println("=== Cancel 阶段：解冻金额 ===");
        
        try {
            // 从上下文中获取参数
            String accountId = (String) context.getActionContext("accountId");
            Integer amount = (Integer) context.getActionContext("amount");
            
            System.out.println("账户ID: " + accountId + ", 解冻金额: " + amount);
            
            // 1. 检查是否有冻结记录
            Integer frozenAmount = frozenAmounts.get(accountId);
            if (frozenAmount == null || !frozenAmount.equals(amount)) {
                System.out.println("❌ 未找到对应的冻结记录");
                return false;
            }
            
            // 2. 清理冻结记录（在实际项目中，这里应该更新数据库）
            frozenAmounts.remove(accountId);
            
            System.out.println("✅ 金额解冻成功，解冻金额: " + amount);
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Cancel 阶段失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取当前冻结的金额（用于测试）
     */
    public int getFrozenAmount(String accountId) {
        return frozenAmounts.getOrDefault(accountId, 0);
    }
    
    /**
     * 清理所有冻结记录（用于测试）
     */
    public void clearFrozenAmounts() {
        frozenAmounts.clear();
    }
}
