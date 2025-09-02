package simple_seata_demo_01;

import org.apache.seata.core.context.RootContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import simple_seata_demo_01.config.TestConfig;
import simple_seata_demo_01.service.OrderService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分布式事务测试类
 * 测试包含两个分支事务的Seata分布式事务
 */
@DisplayName("分布式事务测试")
@SpringBootTest(classes = {TestConfig.class})
class TwoTxBranchTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    @Qualifier("accountJdbcTemplate")
    private JdbcTemplate accountJdbcTemplate;

    @Autowired
    @Qualifier("inventoryJdbcTemplate")
    private JdbcTemplate inventoryJdbcTemplate;

    @BeforeAll
    static void setup(@Autowired @Qualifier("accountJdbcTemplate") JdbcTemplate accountJdbcTemplate,
                      @Autowired @Qualifier("inventoryJdbcTemplate") JdbcTemplate inventoryJdbcTemplate) {
        try {
            // 等待一段时间，让之前的锁释放
            Thread.sleep(2000);

            // 初始化账户数据
            accountJdbcTemplate.execute("REPLACE INTO account VALUES (1, 'UserA', 1000)");
            accountJdbcTemplate.execute("REPLACE INTO account VALUES (2, 'UserB', 1000)");
            accountJdbcTemplate.execute("REPLACE INTO account VALUES (0, 'System', 10000)");

            // 初始化库存数据
            inventoryJdbcTemplate.execute("REPLACE INTO inventory VALUES (1, 'ProductA', 100)");
            inventoryJdbcTemplate.execute("REPLACE INTO inventory VALUES (2, 'ProductB', 50)");

            System.out.println("数据初始化完成");
        } catch (Exception e) {
            System.out.println("初始化数据时出现异常: " + e.getMessage());
            // 如果出现异常，尝试使用INSERT IGNORE
            try {
                accountJdbcTemplate.execute("INSERT IGNORE INTO account VALUES (1, 'UserA', 1000)");
                accountJdbcTemplate.execute("INSERT IGNORE INTO account VALUES (2, 'UserB', 1000)");
                accountJdbcTemplate.execute("INSERT IGNORE INTO account VALUES (0, 'System', 10000)");

                inventoryJdbcTemplate.execute("INSERT IGNORE INTO inventory VALUES (1, 'ProductA', 100)");
                inventoryJdbcTemplate.execute("INSERT IGNORE INTO inventory VALUES (2, 'ProductB', 50)");
            } catch (Exception ex) {
                System.out.println("备用初始化也失败: " + ex.getMessage());
            }
        }
    }

    @Test
    @DisplayName("测试分布式事务提交 - 两个分支事务都成功")
    void testDistributedTransactionCommit() {
        System.out.println("=== 测试分布式事务提交 ===");

        // 记录初始状态
        int initialBalance = getAccountBalance(1);
        int initialStock = getInventoryStock(1);

        System.out.println("===>初始状态 - 用户1余额: " + initialBalance + ", 产品1库存: " + initialStock);

        // 执行分布式事务：创建订单
        // 1. 从用户1账户扣款100元
        // 2. 减少产品1库存10个
        orderService.createOrder(1, 1, 10, 10);

        // 检查最终状态
        int finalBalance = getAccountBalance(1);
        int finalStock = getInventoryStock(1);

        System.out.println("===>最终状态 - 用户1余额: " + finalBalance + ", 产品1库存: " + finalStock);

        // 验证：两个分支事务都成功
        assertEquals(initialBalance - 100, finalBalance, "用户余额应该减少100");
        assertEquals(initialStock - 10, finalStock, "产品库存应该减少10");

        System.out.println("✅ 分布式事务提交测试通过！");
    }

    @Test
    @DisplayName("测试分布式事务回滚 - 第二个分支事务失败")
    void testDistributedTransactionRollback() {
        System.out.println("=== 测试分布式事务回滚 ===");

        // 记录初始状态
        int initialBalance = getAccountBalance(2);
        int initialStock = getInventoryStock(2);

        System.out.println("初始状态 - 用户2余额: " + initialBalance + ", 产品2库存: " + initialStock);

        // 执行会失败的分布式事务
        try {
            // 这里会失败，因为库存不足
            orderService.createOrderWithRollback(2, 2, 10, 10);
        } catch (RuntimeException e) {
            System.out.println("捕获到异常: " + e.getMessage());
        }

        // 检查最终状态
        int finalBalance = getAccountBalance(2);
        int finalStock = getInventoryStock(2);

        System.out.println("最终状态 - 用户2余额: " + finalBalance + ", 产品2库存: " + finalStock);

        // 验证：两个分支事务都回滚
        assertEquals(initialBalance, finalBalance, "用户余额应该保持不变（回滚）");
        assertEquals(initialStock, finalStock, "产品库存应该保持不变（回滚）");

        System.out.println("✅ 分布式事务回滚测试通过！");
    }

    @Test
    @DisplayName("测试分支事务的XID传播")
    void testBranchTransactionXidPropagation() {
        System.out.println("=== 测试分支事务XID传播 ===");

        // 检查初始状态
        String initialXid = RootContext.getXID();
        System.out.println("测试前XID: " + (initialXid != null ? initialXid : "null"));

        try {
            // 执行分布式事务
            orderService.createOrder(1, 1, 5, 10);
        } catch (Exception e) {
            System.out.println("执行过程中出现异常: " + e.getMessage());
        }

        // 检查最终状态
        String finalXid = RootContext.getXID();
        System.out.println("测试后XID: " + (finalXid != null ? finalXid : "null"));

        System.out.println("✅ XID传播测试完成！");
    }

    @Test
    @DisplayName("测试两个数据源的独立性")
    void testDataSourceIndependence() {
        System.out.println("=== 测试两个数据源的独立性 ===");

        // 测试账户数据源
        int accountCount = accountJdbcTemplate.queryForObject("SELECT COUNT(*) FROM account", Integer.class);
        System.out.println("账户表记录数: " + accountCount);

        // 测试库存数据源
        int inventoryCount = inventoryJdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory", Integer.class);
        System.out.println("库存表记录数: " + inventoryCount);

        // 验证两个数据源都能正常工作
        assertTrue(accountCount > 0, "账户数据源应该正常工作");
        assertTrue(inventoryCount > 0, "库存数据源应该正常工作");

        System.out.println("✅ 数据源独立性测试通过！");
    }

    @Test
    @DisplayName("测试分布式事务的隔离性")
    void testDistributedTransactionIsolation() {
        System.out.println("=== 测试分布式事务隔离性 ===");

        // 记录初始状态
        int initialBalance = getAccountBalance(1);
        int initialStock = getInventoryStock(1);

        System.out.println("初始状态 - 用户1余额: " + initialBalance + ", 产品1库存: " + initialStock);

        // 模拟并发操作：一个成功，一个失败
        try {
            // 第一个操作：正常订单
            orderService.createOrder(1, 1, 5, 10);

            // 第二个操作：会失败的订单（库存不足）
            orderService.createOrderWithRollback(1, 1, 1000, 10);
        } catch (RuntimeException e) {
            System.out.println("捕获到异常: " + e.getMessage());
        }

        // 检查最终状态
        int finalBalance = getAccountBalance(1);
        int finalStock = getInventoryStock(1);

        System.out.println("最终状态 - 用户1余额: " + finalBalance + ", 产品1库存: " + finalStock);

        // 验证：只有第一个操作生效，第二个操作回滚
        assertEquals(initialBalance - 50, finalBalance, "只有第一个订单的扣款生效");
        assertEquals(initialStock - 5, finalStock, "只有第一个订单的库存减少生效");

        System.out.println("✅ 分布式事务隔离性测试通过！");
    }

    // 辅助方法：获取账户余额
    private int getAccountBalance(int userId) {
        return accountJdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE id = ?",
                Integer.class,
                userId
        );
    }

    // 辅助方法：获取库存数量
    private int getInventoryStock(int productId) {
        return inventoryJdbcTemplate.queryForObject(
                "SELECT stock FROM inventory WHERE product_id = ?",
                Integer.class,
                productId
        );
    }
}
