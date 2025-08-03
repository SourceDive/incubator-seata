package simple_seata_demo_01.config;

import org.springframework.jdbc.core.JdbcTemplate;
import simple_seata_demo_01.service.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

public class TestConfig {
    @Bean
    public DataSource dataSource() {
        // 配置H2内存数据库
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        // DB_CLOSE_DELAY=-1：保持内存数据库持久化
        // MODE=MySQL：兼容 MySQL 语法
        ds.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
        ds.setUsername("sa"); // 默认为sa
        ds.setPassword("");   // 默认为空
        return ds;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public AccountService accountService(JdbcTemplate jdbcTemplate) {
        return new AccountService(jdbcTemplate);
    }
}