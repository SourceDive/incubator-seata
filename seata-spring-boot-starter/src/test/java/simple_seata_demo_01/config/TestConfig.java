package simple_seata_demo_01.config;

import org.springframework.jdbc.core.JdbcTemplate;
import simple_seata_demo_01.service.AccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class TestConfig {
    @Bean
    public DataSource dataSource() {
        // 配置H2内存数据库，但使用MySQL模式
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        // DB_CLOSE_DELAY=-1：保持内存数据库持久化
        // MODE=MySQL：兼容 MySQL 语法，这是关键！
        ds.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        ds.setUsername("sa"); // 默认为sa
        ds.setPassword("");   // 默认为空
        
        // 直接返回普通DataSource，不使用DataSourceProxy
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