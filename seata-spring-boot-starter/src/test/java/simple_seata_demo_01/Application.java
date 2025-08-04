// 文件路径：seata-spring-boot-starter/src/test/java/io/seata/spring/boot/autoconfigure/SeataH2DemoTest.java

package simple_seata_demo_01;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.rm.datasource.undo.UndoLogManager;
import org.apache.seata.rm.datasource.undo.UndoLogManagerFactory;
import org.apache.seata.rm.datasource.undo.mysql.MySQLUndoLogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import simple_seata_demo_01.config.TestConfig;
import simple_seata_demo_01.service.AccountService;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 快速验证 Seata 的基本工作原理
 * 25.08.01 Fri
 */
@SpringBootTest(classes = {TestConfig.class}) // 告诉 spring 加载 AccountService
class Application {

    @Autowired
    private AccountService accountService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 每个测试前重建表结构
    @BeforeAll
    static void setup(@Autowired DataSource dataSource, @Autowired JdbcTemplate jdbcTemplate) throws SQLException {
        // 初始化H2数据库表结构
        jdbcTemplate.execute("CREATE TABLE if not exists account (id INT PRIMARY KEY, name VARCHAR(50), balance INT)");

        // 必须创建 undo_log 表（AT模式核心）
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS undo_log (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "branch_id BIGINT NOT NULL," +
                "xid VARCHAR(100) NOT NULL," +
                "context VARCHAR(128) NOT NULL," +
                "rollback_info LONGBLOB NOT NULL," +
                "log_status INT NOT NULL," +
                "log_created DATETIME NOT NULL," +
                "log_modified DATETIME NOT NULL," +
                "UNIQUE KEY ux_undo_log (xid, branch_id)" +
                ")");

        jdbcTemplate.update("delete from account");
        jdbcTemplate.execute("INSERT INTO account VALUES (1, 'UserA', 1000)"); // 01. 初始： A 1000元, B 1000元
        jdbcTemplate.execute("INSERT INTO account VALUES (2, 'UserB', 1000)");
    }

    // OK
    @Test
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
     * 下面这个案例回滚不成功啊。
     */
    @Test
    void testRollback() {
        // 转账前
        int balanceA = getBalance(1);
        int balanceB = getBalance(2);

        assertThrows(RuntimeException.class, () ->
                accountService.transferRollback(1, 2, 100)
        );

        // 转账后
        balanceA = getBalance(1);
        balanceB = getBalance(2);

        assertEquals(1000, balanceA);
        assertEquals(1000, balanceB);
    }


    @PostConstruct
    void startH2Console() throws SQLException {
        org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
    }

    private int getBalance(int id) {
        return jdbcTemplate.queryForObject("SELECT balance FROM account WHERE id=?", Integer.class, id);
    }

    @Test
    public void testUndoLogManager() {
        UndoLogManager undoLogManager = UndoLogManagerFactory.getUndoLogManager("mysql");
        assertEquals(MySQLUndoLogManager.class, undoLogManager.getClass());
    }
}