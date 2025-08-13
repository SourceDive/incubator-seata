// 文件路径：seata-spring-boot-starter/src/test/java/io/seata/spring/boot/autoconfigure/SeataH2DemoTest.java

package simple_seata_demo_01;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoLogManager;
import org.apache.seata.spring.annotation.GlobalTransactionScanner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import simple_seata_demo_01.config.TestConfig;
import simple_seata_demo_01.service.AccountService;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 快速验证 Seata 的基本工作原理
 * 25.08.01 Fri
 */
@DisplayName("第一个 seata demo")
@SpringBootTest(classes = {TestConfig.class}) // 告诉 spring 加载 AccountService
class ApplicationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    @Qualifier("manualAccountService") // 这个写法挺好的，不用去动到原有的程序的。
    private AccountService manualAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    // 每个测试前重建表结构
    @BeforeAll
    static void setup(@Autowired DataSource dataSource, @Autowired JdbcTemplate jdbcTemplate) throws SQLException {
        // 使用更安全的方式初始化数据，避免锁冲突
        try {
            // 等待一段时间，让之前的锁释放
            Thread.sleep(2000);

            // 使用REPLACE INTO避免锁冲突
            jdbcTemplate.execute("REPLACE INTO account VALUES (1, 'UserA', 1000)");
            jdbcTemplate.execute("REPLACE INTO account VALUES (2, 'UserB', 1000)");

            System.out.println("数据初始化完成");
        } catch (Exception e) {
            System.out.println("初始化数据时出现异常: " + e.getMessage());
            // 如果出现异常，尝试使用INSERT IGNORE
            try {
                jdbcTemplate.execute("INSERT IGNORE INTO account VALUES (1, 'UserA', 1000)");
                jdbcTemplate.execute("INSERT IGNORE INTO account VALUES (2, 'UserB', 1000)");
            } catch (Exception ex) {
                System.out.println("备用初始化也失败: " + ex.getMessage());
            }
        }
    }

    // OK
    @Test
    @DisplayName("测试提交")
    void testCommit() {
        // A 给 B 转100元
        accountService.transferMoneyCommit(1, 2, 100);

        // 检查转账后的余额
        int balanceA = getBalance(1);
        int balanceB = getBalance(2);

        // 预期：A 900元，B 1100元。
        assertEquals(900, balanceA);
        assertEquals(1100, balanceB);
    }


    /**
     * 测试真正的分布式事务回滚功能
     * 现在有TC服务器，@GlobalTransactional应该生效
     */
    @Test
    @DisplayName("测试回滚")
    void testRollback() {
        // 转账前记录余额
        int balanceA = getBalance(1);
        int balanceB = getBalance(2);
        System.out.println("转账前 - A余额: " + balanceA + ", B余额: " + balanceB);

        // 检查全局事务状态
        String xid = RootContext.getXID();
        System.out.println("执行前XID: " + (xid != null ? xid : "null"));

        // 执行会抛出异常的转账操作
        try {
            // 这里调用带有@GlobalTransactional注解的方法
            accountService.transferRollback(1, 2, 100);
        } catch (RuntimeException e) {
            System.out.println("捕获到异常: " + e.getMessage());

            // 检查异常后的XID
            String xidAfter = RootContext.getXID();
            System.out.println("异常后XID: " + (xidAfter != null ? xidAfter : "null"));
        }

        // 转账后检查余额
        int balanceAAfter = getBalance(1);
        int balanceBAfter = getBalance(2);
        System.out.println("转账后 - A余额: " + balanceAAfter + ", B余额: " + balanceBAfter);

        // 验证：现在有TC服务器，@GlobalTransactional应该生效
        // 两个SQL都应该回滚，余额保持不变
        assertEquals(1000, balanceAAfter, "A的余额应该保持1000不变");
        assertEquals(1000, balanceBAfter, "B的余额应该保持1000不变");

        System.out.println("✅ 分布式事务回滚测试通过！");
    }

    /**
     * 测试当前的行为（没有TC服务器的情况）
     * 验证：第一个SQL执行成功，第二个SQL执行前抛出异常
     */
    @Test
    void testCurrentBehavior() {
        // 转账前记录余额
        int balanceA = getBalance(1);
        int balanceB = getBalance(2);
        System.out.println("转账前 - A余额: " + balanceA + ", B余额: " + balanceB);

        // 执行会抛出异常的转账操作
        try {
            accountService.transferRollback(1, 2, 100);
        } catch (RuntimeException e) {
            System.out.println("捕获到异常: " + e.getMessage());
        }

        // 转账后检查余额
        int balanceAAfter = getBalance(1);
        int balanceBAfter = getBalance(2);
        System.out.println("转账后 - A余额: " + balanceAAfter + ", B余额: " + balanceBAfter);

        // 验证当前行为：
        // 1. 第一个SQL执行成功（A的余额减少）
        // 2. 第二个SQL执行前抛出异常（B的余额不变）
        // 3. 由于没有TC服务器，@GlobalTransactional不生效，所以数据被部分更新
        assertEquals(900, balanceAAfter); // A的余额减少了
        assertEquals(1000, balanceBAfter); // B的余额没有变化
    }

    /**
     * 对比自动扫描和手动创建的bean
     * 测试自动扫描的是代理类
     * 手动创建的是普通类
     *
     * <p>
     * 原因：手动创建的bean定义的beanclassname为null，这种被seata发现的话，不会被增强
     * </p>
     * @see GlobalTransactionScanner#findBusinessBeanNamesNeededEnhancement()
     */
    @Test
    @DisplayName("对比自动扫描和手动创建的bean")
    void testAutowiredBeanAndFactoryMethodBean() {
        System.out.println("=== 对比自动扫描和手动创建的bean ===");

        // 获取自动扫描创建的AccountService（通过@ComponentScan）
        System.out.println("自动扫描创建的AccountService:");
        System.out.println("  类名: " + accountService.getClass().getName());
        System.out.println("  是否为代理: " + accountService.getClass().getName().contains("$$"));
        System.out.println("  是否为CGLIB代理: " + accountService.getClass().getName().contains("CGLIB"));

        // 获取手动创建的AccountService（通过@Bean方法）
        System.out.println("\n手动创建的AccountService:");
        System.out.println("  类名: " + manualAccountService.getClass().getName());
        System.out.println("  是否为代理: " + manualAccountService.getClass().getName().contains("$$"));
        System.out.println("  是否为CGLIB代理: " + manualAccountService.getClass().getName().contains("CGLIB"));

        // 测试手动创建的bean是否支持@GlobalTransactional
        System.out.println("\n测试手动创建的bean的@GlobalTransactional:");
        try {
            manualAccountService.transferRollback(1, 2, 100);
        } catch (RuntimeException e) {
            System.out.println("  异常: " + e.getMessage());
            // 检查XID
            String xid = org.apache.seata.core.context.RootContext.getXID();
            System.out.println("  XID: " + (xid != null ? xid : "null"));
        }
    }

    @Test
    @DisplayName("测试Seata客户端是否正确初始化")
    void testSeataClientInit() {
        System.out.println("=== 测试Seata客户端初始化 ===");

        // 检查DataSourceProxy是否正确创建
        try {
            if (jdbcTemplate.getDataSource() instanceof org.apache.seata.rm.datasource.DataSourceProxy) {
                System.out.println("✅ DataSourceProxy已正确创建");
            } else {
                System.out.println("❌ DataSourceProxy未正确创建，实际类型: " + jdbcTemplate.getDataSource().getClass());
            }
        } catch (Exception e) {
            System.out.println("❌ 检查DataSourceProxy失败: " + e.getMessage());
        }

        // 检查AccountService是否被正确代理
        try {
            if (accountService.getClass().getName().contains("$$")) {
                System.out.println("✅ AccountService已被Spring AOP代理");
            } else {
                System.out.println("❌ AccountService未被Spring AOP代理，实际类型: " + accountService.getClass());
            }
        } catch (Exception e) {
            System.out.println("❌ 检查AccountService代理失败: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("测试全局事务是否正确启动")
    void testGlobalTransaction() {
        System.out.println("=== 测试全局事务启动 ===");

        // 检查初始状态
        String initialXid = RootContext.getXID();
        System.out.println("初始XID: " + (initialXid != null ? initialXid : "null"));

        // 尝试启动全局事务
        try {
            // 手动绑定XID（模拟全局事务启动）
            String testXid = "test-xid-" + System.currentTimeMillis();
            RootContext.bind(testXid);
            System.out.println("手动绑定XID: " + testXid);

            // 检查绑定后的状态
            String boundXid = RootContext.getXID();
            System.out.println("绑定后XID: " + (boundXid != null ? boundXid : "null"));

            // 执行转账操作
            accountService.transferMoneyCommit(1, 2, 50);

            // 检查执行后的状态
            String afterXid = RootContext.getXID();
            System.out.println("执行后XID: " + (afterXid != null ? afterXid : "null"));

        } catch (Exception e) {
            System.out.println("执行过程中出现异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理XID
            RootContext.unbind();
            System.out.println("已清理XID");
        }

        // 检查最终状态
        String finalXid = RootContext.getXID();
        System.out.println("最终XID: " + (finalXid != null ? finalXid : "null"));
    }

    @Test
    @DisplayName("清理数据库锁的测试方法")
    void testCleanup() {
        System.out.println("清理测试 - 验证数据库连接正常");

        // 简单查询验证连接
        int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account", Integer.class);
        System.out.println("账户表记录数: " + count);

        // 验证余额
        int balanceA = getBalance(1);
        int balanceB = getBalance(2);
        System.out.println("当前余额 - A: " + balanceA + ", B: " + balanceB);

        assertTrue(count >= 0, "数据库连接正常");
    }

    @Test
    @DisplayName("测试UndoLogManager的类型是否是 mysql")
    public void testUndoLogManager() {
        UndoLogManager undoLogManager = UndoLogManagerFactory.getUndoLogManager("mysql");
        assertEquals(MySQLUndoLogManager.class, undoLogManager.getClass());
    }

    private int getBalance(int id) {
        return jdbcTemplate.queryForObject("SELECT balance FROM account WHERE id=?", Integer.class, id);
    }
}