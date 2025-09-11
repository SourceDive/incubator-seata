package simple_tcc_demo_01;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * 数据库初始化器
 * 负责在测试前初始化数据库和表结构
 */
@Component
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 初始化数据库
     */
    public void initializeDatabase() {
        System.out.println("=== 开始初始化数据库 ===");
        
        try {
            // 1. 创建数据库
            createDatabase();
            
            // 2. 创建表结构
            createTables();
            
            // 3. 插入测试数据
            insertTestData();
            
            // 4. 验证初始化结果
            verifyInitialization();
            
            System.out.println("✅ 数据库初始化完成");
            
        } catch (Exception e) {
            System.err.println("❌ 数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    /**
     * 创建数据库
     */
    private void createDatabase() {
        System.out.println("创建数据库: seata_tcct_20250911");
        
        // 注意：这里需要先连接到默认数据库来创建目标数据库
        try {
            jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS seata_tcct_20250911 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("✅ 数据库创建成功");
        } catch (Exception e) {
            System.out.println("⚠️ 数据库可能已存在: " + e.getMessage());
        }
    }

    /**
     * 创建表结构
     */
    private void createTables() {
        System.out.println("创建表结构...");
        
        // 创建账户表
        String createAccountTable = "CREATE TABLE IF NOT EXISTS account (" +
            "id INT PRIMARY KEY," +
            "name VARCHAR(50) NOT NULL," +
            "balance INT NOT NULL DEFAULT 0," +
            "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        
        jdbcTemplate.execute(createAccountTable);
        System.out.println("✅ 账户表创建成功");
        
        // 创建 TCC 记录表
        String createTccRecordTable = "CREATE TABLE IF NOT EXISTS tcc_record (" +
            "id VARCHAR(64) PRIMARY KEY," +
            "xid VARCHAR(128) NOT NULL," +
            "branch_id BIGINT NOT NULL," +
            "account_id VARCHAR(32) NOT NULL," +
            "amount INT NOT NULL," +
            "status VARCHAR(16) NOT NULL DEFAULT 'TRY'," +
            "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "INDEX idx_xid (xid)," +
            "INDEX idx_account_id (account_id)," +
            "INDEX idx_status (status)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
        
        jdbcTemplate.execute(createTccRecordTable);
        System.out.println("✅ TCC记录表创建成功");
    }

    /**
     * 插入测试数据
     */
    private void insertTestData() {
        System.out.println("插入测试数据...");
        
        // 清空现有数据
        jdbcTemplate.execute("DELETE FROM account");
        jdbcTemplate.execute("DELETE FROM tcc_record");
        
        // 插入账户数据
        jdbcTemplate.execute("INSERT INTO account (id, name, balance) VALUES (1, 'UserA', 1000)");
        jdbcTemplate.execute("INSERT INTO account (id, name, balance) VALUES (2, 'UserB', 1000)");
        jdbcTemplate.execute("INSERT INTO account (id, name, balance) VALUES (3, 'UserC', 500)");
        jdbcTemplate.execute("INSERT INTO account (id, name, balance) VALUES (0, 'System', 10000)");
        
        System.out.println("✅ 测试数据插入成功");
    }

    /**
     * 验证初始化结果
     */
    private void verifyInitialization() {
        System.out.println("验证初始化结果...");
        
        // 检查表是否存在
        Integer accountCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'seata_tcct_20250911' AND table_name = 'account'", 
            Integer.class
        );
        
        Integer tccRecordCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'seata_tcct_20250911' AND table_name = 'tcc_record'", 
            Integer.class
        );
        
        if (accountCount == null || accountCount == 0) {
            throw new RuntimeException("账户表创建失败");
        }
        
        if (tccRecordCount == null || tccRecordCount == 0) {
            throw new RuntimeException("TCC记录表创建失败");
        }
        
        // 检查测试数据
        Integer dataCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account", Integer.class);
        if (dataCount == null || dataCount < 4) {
            throw new RuntimeException("测试数据插入失败");
        }
        
        System.out.println("✅ 初始化验证通过");
        System.out.println("   - 账户表: " + accountCount + " 个");
        System.out.println("   - TCC记录表: " + tccRecordCount + " 个");
        System.out.println("   - 测试数据: " + dataCount + " 条");
    }

    /**
     * 清理测试数据
     */
    public void cleanupTestData() {
        System.out.println("清理测试数据...");
        
        try {
            jdbcTemplate.execute("DELETE FROM tcc_record");
            jdbcTemplate.execute("DELETE FROM account");
            System.out.println("✅ 测试数据清理完成");
        } catch (Exception e) {
            System.out.println("⚠️ 清理测试数据时出现异常: " + e.getMessage());
        }
    }

    /**
     * 显示当前数据库状态
     */
    public void showDatabaseStatus() {
        System.out.println("\n=== 数据库状态 ===");
        
        try {
            // 显示表列表
            System.out.println("表列表:");
            jdbcTemplate.query("SHOW TABLES", rs -> {
                System.out.println("  - " + rs.getString(1));
            });
            
            // 显示账户数据
            System.out.println("\n账户数据:");
            jdbcTemplate.query("SELECT id, name, balance FROM account ORDER BY id", rs -> {
                System.out.println("  - ID: " + rs.getInt("id") + 
                    ", 姓名: " + rs.getString("name") + 
                    ", 余额: " + rs.getInt("balance"));
            });
            
            // 显示TCC记录
            Integer tccCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tcc_record", Integer.class);
            System.out.println("\nTCC记录数量: " + (tccCount != null ? tccCount : 0));
            
        } catch (Exception e) {
            System.out.println("❌ 获取数据库状态失败: " + e.getMessage());
        }
    }
}
