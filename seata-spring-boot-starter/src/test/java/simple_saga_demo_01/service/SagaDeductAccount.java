package simple_saga_demo_01.service;

import org.apache.seata.rm.tcc.api.BusinessActionContext;

/**
 * @author zero
 * @description todo
 * @date 2025-09-17
 */
public interface SagaDeductAccount {

    void commit(String productId, int quantity,
                BusinessActionContext context);
    void rollback(BusinessActionContext context);
}
