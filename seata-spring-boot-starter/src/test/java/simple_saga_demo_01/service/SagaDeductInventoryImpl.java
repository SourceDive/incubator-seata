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
public class SagaDeductInventoryImpl implements SagaDeductInventory {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 扣减库存 - Saga正向操作
     */
    @CompensationBusinessAction(
            name = "deductInventory",
            compensationMethod = "rollback")
    @Override
    public void commit(String productId, int quantity,
                         BusinessActionContext context) {
        // 保存参数到上下文
        if (context != null) {
            Map<String, Object> actionContext = new HashMap<>();
            actionContext.put("productId", productId);
            actionContext.put("quantity", quantity);
            context.setActionContext(actionContext);
        }

        System.out.println("=== Saga正向操作：扣减库存 ===");
        System.out.println("商品ID: " + productId + ", 数量: " + quantity);

        String sql = "UPDATE inventory SET stock = stock - ? WHERE product_id = ? AND stock >= ?";
        int rows = jdbcTemplate.update(sql, quantity, productId, quantity);

        if (rows == 0) {
            throw new RuntimeException("库存不足，无法扣减");
        }


        System.out.println("库存扣减成功");
    }

    /**
     * 补偿操作：恢复库存
     */
    @Override
    public void rollback(BusinessActionContext context) {
        System.out.println("=== Saga补偿操作：恢复库存 ===");

        String productId = (String) context.getActionContext("productId");
        Integer quantity = (Integer) context.getActionContext("quantity");
        System.out.println("恢复商品ID: " + productId + ", 数量: " + quantity);

        String sql = "UPDATE inventory SET stock = stock + ? WHERE product_id = ?";
        int rows = jdbcTemplate.update(sql, quantity, productId);

        System.out.println("库存恢复完成，影响行数: " + rows);
    }

}