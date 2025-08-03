// 文件路径：seata-spring-boot-starter/src/test/java/io/seata/spring/boot/autoconfigure/SeataH2DemoTest.java

package simple_seata_demo_01;

import org.jetbrains.annotations.Nullable;
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
    static void setup(@Autowired DataSource dataSource) throws SQLException {
        // 初始化H2数据库表结构
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE account (id INT PRIMARY KEY, name VARCHAR(50), balance INT)");
        jdbc.execute("INSERT INTO account VALUES (1, 'UserA', 1000)"); // 01. 初始： A 1000元, B 1000元
        jdbc.execute("INSERT INTO account VALUES (2, 'UserB', 1000)");
    }

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


    @Test
    void testRollback() {
//        assertThrows(RuntimeException.class, () ->
//                accountService.transferRollback(1, 2, 100)
//        );

//        JdbcTemplate jdbc = new JdbcTemplate(accountService.dataSource);
//        int balanceA = getBalance(jdbc);
//        int balanceB = jdbc.queryForObject("SELECT balance FROM account WHERE id=2", Integer.class);

//        assertEquals(1000, balanceA);
//        assertEquals(1000, balanceB);
    }


    @PostConstruct
    void startH2Console() throws SQLException {
        org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
    }

    @Nullable
    private Integer getBalance(int id) {
        return jdbcTemplate.queryForObject("SELECT balance FROM account WHERE id=?", Integer.class, id);
    }
}