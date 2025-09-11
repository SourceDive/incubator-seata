package simple_at_demo_01.config;

import org.apache.seata.rm.datasource.DataSourceProxy;
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
@ComponentScan("simple_at_demo_01.service") // 扫描service包
public class TestConfig {
    
    // 第一个数据源 - 账户服务数据库
    @Bean("accountDataSource")
    public DataSource accountDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://127.0.0.1:3306/seata_test_20250804?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        ds.setUsername("root");
        ds.setPassword("mysql123");

        // 使用DataSourceProxy包装，启用Seata AT模式
        return new DataSourceProxy(ds, "mysql");
    }
    
    // 第二个数据源 - 库存服务数据库
    @Bean("inventoryDataSource")
    public DataSource inventoryDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://127.0.0.1:3306/seata_inventory_test?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        ds.setUsername("root");
        ds.setPassword("mysql123");
        
        // 使用DataSourceProxy包装，启用Seata AT模式
        return new DataSourceProxy(ds, "mysql");
    }

    // 账户服务的JdbcTemplate
    @Bean("accountJdbcTemplate")
    public JdbcTemplate accountJdbcTemplate() {
        return new JdbcTemplate(accountDataSource());
    }
    
    // 库存服务的JdbcTemplate
    @Bean("inventoryJdbcTemplate")
    public JdbcTemplate inventoryJdbcTemplate() {
        return new JdbcTemplate(inventoryDataSource());
    }

    // 主数据源（保持兼容性）
    @Bean
    @Primary
    public DataSource dataSource() {
        return accountDataSource();
    }

    // 主JdbcTemplate（保持兼容性）
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    // 事务管理器
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
    
    // 临时添加：手动创建的AccountService（用于对比）
//    @Bean("manualAccountService")
//    public simple_seata_demo_01.service.AccountService manualAccountService(JdbcTemplate jdbcTemplate) {
//        return new simple_seata_demo_01.service.AccountService(jdbcTemplate);
//    }
}