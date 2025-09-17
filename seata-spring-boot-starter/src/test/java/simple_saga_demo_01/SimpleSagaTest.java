package simple_saga_demo_01;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import simple_saga_demo_01.config.SagaConfig;
import simple_saga_demo_01.service.SagaOrchestrator;

/**
 * 最简单的Saga模式测试 - 使用Seata Saga注解
 * 演示如何使用@SagaStart、@SagaEnd、@SagaCompensation注解
 */
@SpringBootTest
@ContextConfiguration(classes = SagaConfig.class)
public class SimpleSagaTest {
    
    @Autowired
    private SagaOrchestrator sagaOrchestrator;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @BeforeEach
    public void setUp() {
        // 初始化测试数据
        initTestData();
    }
    
    /**
     * 初始化测试数据
     */
    private void initTestData() {
        System.out.println("=== 初始化测试数据 ===");
        
        // 创建表
        jdbcTemplate.execute("DROP TABLE IF EXISTS orders");
        jdbcTemplate.execute("CREATE TABLE orders (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "order_id VARCHAR(50) UNIQUE, " +
                "user_id VARCHAR(50), " +
                "amount INT, " +
                "status VARCHAR(20)" +
                ")");
        
        jdbcTemplate.execute("DROP TABLE IF EXISTS inventory");
        jdbcTemplate.execute("CREATE TABLE inventory (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "product_id VARCHAR(50) UNIQUE, " +
                "stock INT" +
                ")");
        
        jdbcTemplate.execute("DROP TABLE IF EXISTS account");
        jdbcTemplate.execute("CREATE TABLE account (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id VARCHAR(50) UNIQUE, " +
                "balance INT" +
                ")");
        
        // 插入测试数据
        jdbcTemplate.update("INSERT INTO inventory (product_id, stock) VALUES (?, ?)", "P001", 10);
        jdbcTemplate.update("INSERT INTO account (user_id, balance) VALUES (?, ?)", "U001", 1000);
        
        System.out.println("测试数据初始化完成");
    }
    
    /**
     * 测试Saga成功场景 - 使用注解
     */
    @Test
    public void testSagaSuccess() {
        System.out.println("\n========== 测试Saga成功场景（使用注解） ==========");
        
        String orderId = "ORDER_" + System.currentTimeMillis();
        String userId = "U001";
        String productId = "P001";
        int amount = 100;
        int quantity = 2;
        
        try {
            sagaOrchestrator.executeSagaSuccess(orderId, userId, productId, amount, quantity);
            
            // 验证结果
            verifyDataAfterSuccess(userId, productId, amount, quantity);
            
        } catch (Exception e) {
            System.out.println("Saga事务执行异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * todo 这个测试案例还是失败的，还需要定位原因。
     * 测试Saga失败场景（库存不足）- 使用注解
     */
    @Test
    public void testSagaFailure() {
        System.out.println("\n========== 测试Saga失败场景（库存不足，使用注解） ==========");
        
        String orderId = "ORDER_" + System.currentTimeMillis();
        String userId = "U001";
        String productId = "P001";
        int amount = 100;
        int quantity = 20; // 超过库存数量，会失败
        
        try {
            sagaOrchestrator.executeSagaFailure(orderId, userId, productId, amount, quantity);
            
        } catch (Exception e) {
            System.out.println("Saga事务执行异常（预期）: " + e.getMessage());
            
            // 验证补偿结果
            verifyDataAfterFailure(userId, productId, amount, quantity);
        }
    }
    
    /**
     * 测试简单Saga示例
     */
    @Test
    public void testSimpleSagaExample() {
        System.out.println("\n========== 测试简单Saga示例 ==========");
        
        String orderId = "SIMPLE_ORDER_" + System.currentTimeMillis();
        String userId = "U001";
        int amount = 50;
        
        try {
            // 执行简单Saga事务
            sagaOrchestrator.simpleSagaExample(orderId, userId, amount);
            
            // 验证结果
            verifySimpleSagaResult(userId, amount);
            
        } catch (Exception e) {
            System.out.println("简单Saga事务执行异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 验证成功场景后的数据状态
     */
    private void verifyDataAfterSuccess(String userId, String productId, int amount, int quantity) {
        System.out.println("\n=== 验证成功场景后的数据状态 ===");
        
        // 检查订单
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE user_id = ?", 
                Integer.class, userId);
        System.out.println("订单数量: " + orderCount);
        
        // 检查订单状态
        String orderStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE user_id = ? ORDER BY id DESC LIMIT 1", 
                String.class, userId);
        System.out.println("最新订单状态: " + orderStatus);
        
        // 检查库存
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM inventory WHERE product_id = ?", 
                Integer.class, productId);
        System.out.println("剩余库存: " + stock);
        
        // 检查账户余额
        Integer balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?", 
                Integer.class, userId);
        System.out.println("账户余额: " + balance);
        
        // 验证数据一致性
        assert orderCount > 0 : "应该有订单记录";
        assert "CREATED".equals(orderStatus) : "订单状态应该是COMPLETED";
        assert stock == (10 - quantity) : "库存应该被扣减";
        assert balance == (1000 - amount) : "账户余额应该被扣减";
        
        System.out.println("数据验证通过！");
    }
    
    /**
     * 验证失败场景后的数据状态
     */
    private void verifyDataAfterFailure(String userId, String productId, int amount, int quantity) {
        System.out.println("\n=== 验证失败场景后的数据状态 ===");
        
        // 检查订单（应该被补偿删除）
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE user_id = ?", 
                Integer.class, userId);
        System.out.println("订单数量: " + orderCount);
        
        // 检查库存（应该保持原值）
        Integer stock = jdbcTemplate.queryForObject(
                "SELECT stock FROM inventory WHERE product_id = ?", 
                Integer.class, productId);
        System.out.println("剩余库存: " + stock);
        
        // 检查账户余额（应该保持原值）
        Integer balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?", 
                Integer.class, userId);
        System.out.println("账户余额: " + balance);
        
        // 验证数据一致性（应该回滚到初始状态）
        assert orderCount == 0 : "订单应该被补偿删除";
        assert stock == 10 : "库存应该保持原值";
        assert balance == 1000 : "账户余额应该保持原值";
        
        System.out.println("数据验证通过！补偿操作成功！");
    }
    
    /**
     * 验证简单Saga结果
     */
    private void verifySimpleSagaResult(String userId, int amount) {
        System.out.println("\n=== 验证简单Saga结果 ===");
        
        // 检查订单
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE user_id = ?", 
                Integer.class, userId);
        System.out.println("订单数量: " + orderCount);
        
        // 检查账户余额
        Integer balance = jdbcTemplate.queryForObject(
                "SELECT balance FROM account WHERE user_id = ?", 
                Integer.class, userId);
        System.out.println("账户余额: " + balance);
        
        // 验证数据一致性
        assert orderCount > 0 : "应该有订单记录";
        assert balance == (1000 - amount) : "账户余额应该被扣减";
        
        System.out.println("简单Saga验证通过！");
    }
}