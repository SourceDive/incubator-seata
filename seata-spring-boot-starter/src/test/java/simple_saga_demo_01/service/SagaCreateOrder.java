package simple_saga_demo_01.service;

import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.saga.rm.api.CompensationBusinessAction;

/**
 * @author zero
 * @description todo
 * @date 2025-09-17
 */
public interface SagaCreateOrder {

    void commit(String orderId, String userId, int amount, BusinessActionContext context);

    void rollback(BusinessActionContext context);
}
