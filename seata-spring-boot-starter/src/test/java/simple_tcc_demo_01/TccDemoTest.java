package simple_tcc_demo_01;

import org.apache.seata.spring.annotation.GlobalTransactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import simple_tcc_demo_01.config.TccConfig;
import simple_tcc_demo_01.service.AccountTccService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCC 模式演示测试类
 * <p>
 * 演示最简单的 TCC 模式使用
 */
@DisplayName("TCC 模式演示测试")
@SpringBootTest(classes = {TccConfig.class, DatabaseInitializer.class})
class TccDemoTest {

    @Autowired
    private AccountTccService accountTccService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @BeforeEach
    void setUp() {
        System.out.println("\n==================================================");
        System.out.println("开始执行测试前初始化");

        // 初始化数据库
        databaseInitializer.initializeDatabase();

        // 清理测试数据
        accountTccService.clearFrozenAmounts();

        System.out.println("测试前初始化完成");
        System.out.println("==================================================");
    }

    @Test
    @DisplayName("测试 TCC 成功场景")
    @GlobalTransactional
    void testTccSuccess() {
        System.out.println("\n=== 测试 TCC 成功场景 ===");

        // 记录初始余额
        int initialBalance = getAccountBalance(1);
        System.out.println("初始余额: " + initialBalance);

        // 执行 TCC 扣款
        boolean result = accountTccService.tryDeduct("1", 100);

        // 验证 Try 阶段成功
        assertTrue(result, "Try 阶段应该成功");

        // 检查冻结金额
        int frozenAmount = accountTccService.getFrozenAmount("1");
        assertEquals(100, frozenAmount, "冻结金额应该是 100");

        // 检查账户余额（Try 阶段不应该改变余额）
        int balanceAfterTry = getAccountBalance(1);
        assertEquals(initialBalance, balanceAfterTry, "Try 阶段后余额不应该改变");

        System.out.println("✅ TCC 成功场景测试通过");
        System.out.println("Try 阶段：冻结金额 " + frozenAmount + "，账户余额保持 " + balanceAfterTry);
    }

    @Test
    @DisplayName("测试 TCC 回滚场景")
    @GlobalTransactional
    void testTccRollback() {
        System.out.println("\n=== 测试 TCC 回滚场景 ===");

        // 记录初始余额
        int initialBalance = getAccountBalance(2);
        System.out.println("初始余额: " + initialBalance);

        try {
            // 执行 TCC 扣款
            boolean result = accountTccService.tryDeduct("2", 100);
            assertTrue(result, "Try 阶段应该成功");

            // 检查冻结金额
            int frozenAmount = accountTccService.getFrozenAmount("2");
            assertEquals(100, frozenAmount, "冻结金额应该是 100");

            // 模拟业务异常，触发回滚
            throw new RuntimeException("模拟业务异常，触发 TCC 回滚");

        } catch (RuntimeException e) {
            System.out.println("捕获到异常: " + e.getMessage());

            // 验证回滚后冻结金额被清理
            int frozenAmountAfterRollback = accountTccService.getFrozenAmount("2");
            assertEquals(0, frozenAmountAfterRollback, "回滚后冻结金额应该为 0");

            // 验证账户余额没有改变
            int balanceAfterRollback = getAccountBalance(2);
            assertEquals(initialBalance, balanceAfterRollback, "回滚后余额应该保持不变");

            System.out.println("✅ TCC 回滚场景测试通过");
            System.out.println("回滚后：冻结金额 " + frozenAmountAfterRollback + "，账户余额 " + balanceAfterRollback);
        }
    }

    @Test
    @DisplayName("测试 TCC 余额不足场景")
    @GlobalTransactional
    void testTccInsufficientBalance() {
        System.out.println("\n=== 测试 TCC 余额不足场景 ===");

        // 记录初始余额
        int initialBalance = getAccountBalance(1);
        System.out.println("初始余额: " + initialBalance);

        // 尝试扣款超过余额的金额
        boolean result = accountTccService.tryDeduct("1", initialBalance + 100);

        // 验证 Try 阶段失败
        assertFalse(result, "余额不足时 Try 阶段应该失败");

        // 验证没有冻结金额
        int frozenAmount = accountTccService.getFrozenAmount("1");
        assertEquals(0, frozenAmount, "余额不足时不应该有冻结金额");

        System.out.println("✅ TCC 余额不足场景测试通过");
    }

    @Test
    @DisplayName("测试 TCC 账户不存在场景")
    @GlobalTransactional
    void testTccAccountNotExists() {
        System.out.println("\n=== 测试 TCC 账户不存在场景 ===");

        // 尝试扣款不存在的账户
        boolean result = accountTccService.tryDeduct("999", 100);

        // 验证 Try 阶段失败
        assertFalse(result, "账户不存在时 Try 阶段应该失败");

        // 验证没有冻结金额
        int frozenAmount = accountTccService.getFrozenAmount("999");
        assertEquals(0, frozenAmount, "账户不存在时不应该有冻结金额");

        System.out.println("✅ TCC 账户不存在场景测试通过");
    }

    @Test
    @DisplayName("测试 TCC 幂等性")
    @GlobalTransactional
    void testTccIdempotency() {
        System.out.println("\n=== 测试 TCC 幂等性 ===");

        // 记录初始余额
        int initialBalance = getAccountBalance(1);
        System.out.println("初始余额: " + initialBalance);

        // 第一次执行 Try
        boolean result1 = accountTccService.tryDeduct("1", 100);
        assertTrue(result1, "第一次 Try 应该成功");

        // 第二次执行 Try（幂等性测试）
        boolean result2 = accountTccService.tryDeduct("1", 100);
        assertTrue(result2, "第二次 Try 应该成功（幂等性）");

        // 验证冻结金额
        int frozenAmount = accountTccService.getFrozenAmount("1");
        assertEquals(100, frozenAmount, "幂等性测试：冻结金额应该是 100");

        System.out.println("✅ TCC 幂等性测试通过");
    }

    @Test
    @DisplayName("显示数据库状态")
    void testShowDatabaseStatus() {
        System.out.println("\n=== 显示数据库状态 ===");
        databaseInitializer.showDatabaseStatus();
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