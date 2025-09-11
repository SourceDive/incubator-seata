package simple_tcc_demo_01.config;

import org.apache.seata.rm.datasource.DataSourceProxy;
import org.apache.seata.spring.annotation.GlobalTransactionScanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * TCC 测试配置类
 */
@Configuration
public class TccConfig {

    /**
     * 数据源配置
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://127.0.0.1:3306/seata_tcct_20250911?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        ds.setUsername("root");
        ds.setPassword("mysql123");
        
        // 使用 DataSourceProxy 包装，启用 Seata
        return new DataSourceProxy(ds, "mysql");
    }

    /**
     * JdbcTemplate 配置
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Seata 全局事务扫描器
     */
    @Bean
    @Primary
    public GlobalTransactionScanner globalTransactionScanner() {
        return new GlobalTransactionScanner("tcc-test", "tcc_test_tx_group");
    }
}
