package simple_saga_demo_01.service;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.saga.rm.api.CompensationBusinessAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单Saga服务
 *
 * <p>这里定义的三对正常操作及补偿操作。</p>
 * <p>
 * 使用@CompensationBusinessAction注解定义补偿操作
 */
@Service
public class SagaCreateOrderImpl implements SagaCreateOrder {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 创建订单 - Saga正向操作
     */
    @CompensationBusinessAction(
            name = "createOrder",
            compensationMethod = "rollback")
    @Override
    public void commit(String orderId, String userId, int amount, BusinessActionContext context) {
        System.out.println("=== Saga正向操作：创建订单 ===");
        System.out.println("订单ID: " + orderId + ", 用户ID: " + userId + ", 金额: " + amount);

        // 保存参数到上下文，用于补偿操作
        if (context != null) {
            Map<String, Object> actionContext = new HashMap<>();
            actionContext.put("orderId", orderId);
            actionContext.put("userId", userId);
            actionContext.put("amount", amount);
            context.setActionContext(actionContext);
        }

        String sql = "INSERT INTO orders (order_id, user_id, amount, status) VALUES (?, ?, ?, 'CREATED')";
        jdbcTemplate.update(sql, orderId, userId, amount);

        System.out.println("订单创建成功");
    }

    /**
     * 补偿操作：删除订单
     */
    @Override
    public void rollback(BusinessActionContext context) {
        System.out.println("=== Saga补偿操作：删除订单 ===");

        String orderId = (String) context.getActionContext("orderId");
        System.out.println("补偿订单ID: " + orderId);

        String sql = "DELETE FROM orders WHERE order_id = ?";
        int rows = jdbcTemplate.update(sql, orderId);

        System.out.println("订单删除完成，影响行数: " + rows);
    }
}