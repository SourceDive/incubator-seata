package simple_saga_demo_01.service;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.saga.rm.api.CompensationBusinessAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单Saga服务 - 使用正确的Seata Saga注解
 * 使用@CompensationBusinessAction注解定义补偿操作
 */
@Service
public class OrderSagaService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 创建订单 - Saga正向操作
     */
    @CompensationBusinessAction(
            name = "createOrder",
            compensationMethod = "compensateCreateOrder")
    public void createOrder(String orderId, String userId, int amount, BusinessActionContext context) {
        System.out.println("=== Saga正向操作：创建订单 ===");
        System.out.println("订单ID: " + orderId + ", 用户ID: " + userId + ", 金额: " + amount);

        String sql = "INSERT INTO orders (order_id, user_id, amount, status) VALUES (?, ?, ?, 'CREATED')";
        jdbcTemplate.update(sql, orderId, userId, amount);

        // 保存参数到上下文，用于补偿操作
        Map<String, Object> actionContext = new HashMap<>();
        actionContext.put("orderId", orderId);
        actionContext.put("userId", userId);
        actionContext.put("amount", amount);
        context.setActionContext(actionContext);

        System.out.println("订单创建成功");
    }

    /**
     * 补偿操作：删除订单
     */
    public void compensateCreateOrder(BusinessActionContext context) {
        System.out.println("=== Saga补偿操作：删除订单 ===");

        String orderId = (String) context.getActionContext("orderId");
        System.out.println("补偿订单ID: " + orderId);

        String sql = "DELETE FROM orders WHERE order_id = ?";
        int rows = jdbcTemplate.update(sql, orderId);

        System.out.println("订单删除完成，影响行数: " + rows);
    }

    /**
     * 扣减库存 - Saga正向操作
     */
    @CompensationBusinessAction(
            name = "deductInventory",
            compensationMethod = "compensateDeductInventory")
    public void deductInventory(String productId, int quantity, BusinessActionContext context) {
        System.out.println("=== Saga正向操作：扣减库存 ===");
        System.out.println("商品ID: " + productId + ", 数量: " + quantity);

        String sql = "UPDATE inventory SET stock = stock - ? WHERE product_id = ? AND stock >= ?";
        int rows = jdbcTemplate.update(sql, quantity, productId, quantity);

        if (rows == 0) {
            throw new RuntimeException("库存不足，无法扣减");
        }

        // 保存参数到上下文
        Map<String, Object> actionContext = new HashMap<>();
        actionContext.put("productId", productId);
        actionContext.put("quantity", quantity);
        context.setActionContext(actionContext);

        System.out.println("库存扣减成功");
    }

    /**
     * 补偿操作：恢复库存
     */
    public void compensateDeductInventory(BusinessActionContext context) {
        System.out.println("=== Saga补偿操作：恢复库存 ===");

        String productId = (String) context.getActionContext("productId");
        Integer quantity = (Integer) context.getActionContext("quantity");
        System.out.println("恢复商品ID: " + productId + ", 数量: " + quantity);

        String sql = "UPDATE inventory SET stock = stock + ? WHERE product_id = ?";
        int rows = jdbcTemplate.update(sql, quantity, productId);

        System.out.println("库存恢复完成，影响行数: " + rows);
    }

    /**
     * 扣减账户余额 - Saga正向操作
     */
    @CompensationBusinessAction(
            name = "deductAccount",
            compensationMethod = "compensateDeductAccount")
    public void deductAccount(String userId, int amount, BusinessActionContext context) {
        System.out.println("=== Saga正向操作：扣减账户余额 ===");
        System.out.println("用户ID: " + userId + ", 金额: " + amount);

        String sql = "UPDATE account SET balance = balance - ? WHERE user_id = ? AND balance >= ?";
        int rows = jdbcTemplate.update(sql, amount, userId, amount);

        if (rows == 0) {
            throw new RuntimeException("账户余额不足");
        }

        // 保存参数到上下文
        Map<String, Object> actionContext = new HashMap<>();
        actionContext.put("userId", userId);
        actionContext.put("amount", amount);
        context.setActionContext(actionContext);

        System.out.println("账户扣减成功");
    }

    /**
     * 补偿操作：恢复账户余额
     */
    public void compensateDeductAccount(BusinessActionContext context) {
        System.out.println("=== Saga补偿操作：恢复账户余额 ===");

        String userId = (String) context.getActionContext("userId");
        Integer amount = (Integer) context.getActionContext("amount");
        System.out.println("恢复用户ID: " + userId + ", 金额: " + amount);

        String sql = "UPDATE account SET balance = balance + ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, amount, userId);

        System.out.println("账户余额恢复完成，影响行数: " + rows);
    }
}