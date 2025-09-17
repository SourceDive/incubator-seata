package simple_saga_demo_01.config;

// Saga模式不需要DataSourceProxy
import org.apache.seata.spring.annotation.GlobalTransactionScanner;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ComponentScan("simple_saga_demo_01.service")
public class SagaConfig {
    
    // 数据源配置
    @Bean("sagaDataSource")
    public DataSource sagaDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/seata_saga_test?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        dataSource.setUsername("root");
        dataSource.setPassword("mysql123");

        // Saga模式：直接返回普通DataSource，不需要DataSourceProxy
        return dataSource;
    }

    @Bean("sagaJdbcTemplate")
    public JdbcTemplate sagaJdbcTemplate() {
        return new JdbcTemplate(sagaDataSource());
    }

//    @Bean
//    @Primary
//    public DataSource dataSource() {
//        return sagaDataSource();
//    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(sagaDataSource());
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    // Seata全局事务扫描器
    @Bean
    @Primary
    public GlobalTransactionScanner globalTransactionScanner() {
        return new GlobalTransactionScanner("seata-test", "my_test_tx_group");
    }
}

