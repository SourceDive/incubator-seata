package simple_tcc_demo_01;

import io.seata.spring.annotation.GlobalTransactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import simple_tcc_demo_01.config.TccConfig;
import simple_tcc_demo_01.service.SimpleTccService;
import simple_tcc_demo_01.DatabaseInitializer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单 TCC 测试 - 单数据库版本
 * 
 * 演示在单个数据库内使用 TCC 模式
 */
@DisplayName("简单 TCC 测试 - 单数据库")
@SpringBootTest(classes = {TccConfig.class})
class SimpleTccTest {

    @Autowired
    private SimpleTccService simpleTccService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @BeforeEach
    void setUp() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("开始执行测试前初始化");
        
        // 初始化数据库
        databaseInitializer.initializeDatabase();
        
        // 清理测试数据
        simpleTccService.clearFrozenRecords();
        
        System.out.println("测试前初始化完成");
        System.out.println("=".repeat(50));
    }

    @Test
    @DisplayName("测试 TCC 转账成功场景")
    @GlobalTransactional
    void testTccTransferSuccess() {
        System.out.println("\n=== 测试 TCC 转账成功场景 ===");
        
        // 记录初始余额
        int fromBalance = getAccountBalance(1);
        int toBalance = getAccountBalance(2);
        System.out.println("转账前 - 账户1余额: " + fromBalance + ", 账户2余额: " + toBalance);
        
        // 执行 TCC 转账
        boolean result = simpleTccService.tryTransfer("1", "2", 200);
        
        // 验证 Try 阶段成功
        assertTrue(result, "Try 阶段应该成功");
        
        // 检查冻结记录
        int frozenCount = simpleTccService.getFrozenRecordCount();
        assertEquals(1, frozenCount, "应该有1条冻结记录");
        
        // 检查账户余额（Try 阶段不应该改变余额）
        int fromBalanceAfterTry = getAccountBalance(1);
        int toBalanceAfterTry = getAccountBalance(2);
        assertEquals(fromBalance, fromBalanceAfterTry, "Try 阶段后转出账户余额不应该改变");
        assertEquals(toBalance, toBalanceAfterTry, "Try 阶段后转入账户余额不应该改变");
        
        System.out.println("✅ TCC 转账成功场景测试通过");
        System.out.println("Try 阶段：冻结记录 " + frozenCount + " 条，账户余额保持不变");
    }

    @Test
    @DisplayName("测试 TCC 转账回滚场景")
    @GlobalTransactional
    void testTccTransferRollback() {
        System.out.println("\n=== 测试 TCC 转账回滚场景 ===");
        
        // 记录初始余额
        int fromBalance = getAccountBalance(1);
        int toBalance = getAccountBalance(2);
        System.out.println("转账前 - 账户1余额: " + fromBalance + ", 账户2余额: " + toBalance);
        
        try {
            // 执行 TCC 转账
            boolean result = simpleTccService.tryTransfer("1", "2", 200);
            assertTrue(result, "Try 阶段应该成功");
            
            // 检查冻结记录
            int frozenCount = simpleTccService.getFrozenRecordCount();
            assertEquals(1, frozenCount, "应该有1条冻结记录");
            
            // 模拟业务异常，触发回滚
            throw new RuntimeException("模拟业务异常，触发 TCC 回滚");
            
        } catch (RuntimeException e) {
            System.out.println("捕获到异常: " + e.getMessage());
            
            // 验证回滚后冻结记录被清理
            int frozenCountAfterRollback = simpleTccService.getFrozenRecordCount();
            assertEquals(0, frozenCountAfterRollback, "回滚后冻结记录应该为 0");
            
            // 验证账户余额没有改变
            int fromBalanceAfterRollback = getAccountBalance(1);
            int toBalanceAfterRollback = getAccountBalance(2);
            assertEquals(fromBalance, fromBalanceAfterRollback, "回滚后转出账户余额应该保持不变");
            assertEquals(toBalance, toBalanceAfterRollback, "回滚后转入账户余额应该保持不变");
            
            System.out.println("✅ TCC 转账回滚场景测试通过");
            System.out.println("回滚后：冻结记录 " + frozenCountAfterRollback + " 条，账户余额保持不变");
        }
    }

    @Test
    @DisplayName("测试 TCC 余额不足场景")
    @GlobalTransactional
    void testTccInsufficientBalance() {
        System.out.println("\n=== 测试 TCC 余额不足场景 ===");
        
        // 记录初始余额
        int fromBalance = getAccountBalance(3);
        System.out.println("账户3余额: " + fromBalance);
        
        // 尝试转账超过余额的金额
        boolean result = simpleTccService.tryTransfer("3", "1", fromBalance + 100);
        
        // 验证 Try 阶段失败
        assertFalse(result, "余额不足时 Try 阶段应该失败");
        
        // 验证没有冻结记录
        int frozenCount = simpleTccService.getFrozenRecordCount();
        assertEquals(0, frozenCount, "余额不足时不应该有冻结记录");
        
        System.out.println("✅ TCC 余额不足场景测试通过");
    }

    @Test
    @DisplayName("测试 TCC 账户不存在场景")
    @GlobalTransactional
    void testTccAccountNotExists() {
        System.out.println("\n=== 测试 TCC 账户不存在场景 ===");
        
        // 尝试从不存在的账户转账
        boolean result = simpleTccService.tryTransfer("999", "1", 100);
        
        // 验证 Try 阶段失败
        assertFalse(result, "账户不存在时 Try 阶段应该失败");
        
        // 验证没有冻结记录
        int frozenCount = simpleTccService.getFrozenRecordCount();
        assertEquals(0, frozenCount, "账户不存在时不应该有冻结记录");
        
        System.out.println("✅ TCC 账户不存在场景测试通过");
    }

    @Test
    @DisplayName("测试 TCC 幂等性")
    @GlobalTransactional
    void testTccIdempotency() {
        System.out.println("\n=== 测试 TCC 幂等性 ===");
        
        // 记录初始余额
        int fromBalance = getAccountBalance(1);
        int toBalance = getAccountBalance(2);
        
        // 第一次执行 Try
        boolean result1 = simpleTccService.tryTransfer("1", "2", 100);
        assertTrue(result1, "第一次 Try 应该成功");
        
        // 第二次执行 Try（幂等性测试）
        boolean result2 = simpleTccService.tryTransfer("1", "2", 100);
        assertTrue(result2, "第二次 Try 应该成功（幂等性）");
        
        // 验证冻结记录
        int frozenCount = simpleTccService.getFrozenRecordCount();
        assertEquals(2, frozenCount, "幂等性测试：应该有2条冻结记录");
        
        System.out.println("✅ TCC 幂等性测试通过");
    }

    /**
     * 获取账户余额
     */
    private int getAccountBalance(int accountId) {
        return jdbcTemplate.queryForObject(
            "SELECT balance FROM account WHERE id = ?", 
            Integer.class, 
            accountId
        );
    }
}
