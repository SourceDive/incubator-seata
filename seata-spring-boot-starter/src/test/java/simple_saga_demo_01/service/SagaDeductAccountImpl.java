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
 *
 * 使用@CompensationBusinessAction注解定义补偿操作
 */
@Service
public class SagaDeductAccountImpl implements SagaDeductAccount {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 扣减账户余额 - Saga正向操作
     */
    @CompensationBusinessAction(
            name = "deductAccount",
            compensationMethod = "rollback")
    @Override
    public void commit(String userId, int amount,
                      BusinessActionContext context) {
        System.out.println("=== Saga正向操作：扣减账户余额 ===");
        System.out.println("用户ID: " + userId + ", 金额: " + amount);

        String sql = "UPDATE account SET balance = balance - ? WHERE user_id = ? AND balance >= ?";
        int rows = jdbcTemplate.update(sql, amount, userId, amount);

        if (rows == 0) {
            throw new RuntimeException("账户余额不足");
        }

        // 保存参数到上下文
        if (context != null) {
            Map<String, Object> actionContext = new HashMap<>();
            actionContext.put("userId", userId);
            actionContext.put("amount", amount);
            context.setActionContext(actionContext);
        }

        System.out.println("账户扣减成功");
    }

    /**
     * 补偿操作：恢复账户余额
     */
    @Override
    public void rollback(BusinessActionContext context) {
        System.out.println("=== Saga补偿操作：恢复账户余额 ===");

        String userId = (String) context.getActionContext("userId");
        Integer amount = (Integer) context.getActionContext("amount");
        System.out.println("恢复用户ID: " + userId + ", 金额: " + amount);

        String sql = "UPDATE account SET balance = balance + ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, amount, userId);

        System.out.println("账户余额恢复完成，影响行数: " + rows);
    }

}