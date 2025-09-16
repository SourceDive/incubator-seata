package simple_tcc_demo_02.service;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * 账户 TCC 服务接口
 * <p>
 * TCC 模式需要实现三个方法：
 * 1. Try: 尝试执行，预留资源
 * 2. Confirm: 确认执行，提交资源
 * 3. Cancel: 取消执行，释放资源
 */
@LocalTCC
public interface AccountTccService {

    /**
     * Try 阶段：冻结账户金额
     *
     * @param accountId 账户ID
     * @param amount    金额
     * @return 是否成功
     */
    @TwoPhaseBusinessAction(
            name = "accountTcc",
            commitMethod = "confirm",
            rollbackMethod = "cancel"
    )
    boolean tryDeduct(String accountId, int amount);

    /**
     * Confirm 阶段：真正扣款
     *
     * @param context 业务上下文
     * @return 是否成功
     */
    boolean confirm(BusinessActionContext context);

    /**
     * Cancel 阶段：解冻金额
     *
     * @param context 业务上下文
     * @return 是否成功
     */
    boolean cancel(BusinessActionContext context);

    /**
     * 获取当前冻结的金额（用于测试）
     *
     * @param accountId 账户ID
     * @return 冻结金额
     */
    int getFrozenAmount(String accountId);

    /**
     * 清理所有冻结记录（用于测试）
     */
    void clearFrozenAmounts();
}
