package simple_tcc_demo_01.config;

// 注意：TCC 模式下不需要 DataSourceProxy
import org.apache.seata.spring.annotation.GlobalTransactionScanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * TCC 测试配置类<br><br>
 * 
 * TCC 模式与 AT 模式的重要区别：<br><br>
 * 
 * 1. AT 模式需要 DataSourceProxy：<br>
 *    - AT 模式依赖自动生成的 UNDO_LOG 实现回滚<br>
 *    - DataSourceProxy 拦截 SQL 语句，自动生成回滚日志<br>
 *    - 事务回滚时自动执行反向 SQL<br><br>
 * 
 * 2. TCC 模式不需要 DataSourceProxy：<br>
 *    - TCC 模式通过业务逻辑实现事务补偿<br>
 *    - Try 阶段：预留资源，不真正执行业务<br>
 *    - Confirm 阶段：真正执行业务逻辑<br>
 *    - Cancel 阶段：通过业务逻辑释放资源<br>
 *    - 完全由开发者控制事务的提交和回滚<br><br>
 * 
 * 3. 性能优势：<br>
 *    - 直接使用原始数据源，减少代理层开销<br>
 *    - 不需要生成和维护 UNDO_LOG<br>
 *    - 更精确的业务控制
 */
@Configuration
public class TccConfig {

    /**
     * 数据源配置 - TCC 模式<br><br>
     * 
     * TCC 模式下直接使用原始数据源，不需要 DataSourceProxy 包装<br>
     * 因为 TCC 通过业务逻辑实现事务补偿，不依赖 AT 模式的自动回滚机制
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/seata_tcct_20250911?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        dataSource.setUsername("root");
        dataSource.setPassword("mysql123");

        // TCC 模式：直接返回原始数据源，不使用 DataSourceProxy
        return dataSource;
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
