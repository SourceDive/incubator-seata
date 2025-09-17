package simple_saga_demo_01.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.seata.spring.annotation.GlobalTransactional;

/**
 * Saga编排器 - 使用@GlobalTransactional注解
 * 演示如何使用Seata的全局事务注解来管理Saga事务
 */
@Service
public class SagaOrchestrator {
    
    @Autowired
    private SagaDeductInventory deductInventory;
    @Autowired
    private SagaDeductAccount deductAccount;
    @Autowired
    private SagaCreateOrder createOrder;

    /**
     * 执行Saga事务 - 成功场景
     * 使用@GlobalTransactional注解管理全局事务
     */
    @GlobalTransactional(rollbackFor = Exception.class)
    public void executeSagaSuccess(String orderId, String userId, String productId, int amount, int quantity) {
        System.out.println("=== 开始执行Saga事务（成功场景） ===");
        
        try {
            // 步骤1：创建订单
            createOrder.commit(orderId, userId, amount, null);
            
            // 步骤2：扣减库存
            deductInventory.commit(productId, quantity, null);
            
            // 步骤3：扣减账户余额
            deductAccount.commit(userId, amount, null);
            
            System.out.println("=== Saga事务执行成功 ===");
            
        } catch (Exception e) {
            System.out.println("=== Saga事务执行失败，Seata将自动执行补偿 ===");
            System.out.println("错误信息: " + e.getMessage());
            throw e; // 重新抛出异常，让Seata处理补偿
        }
    }
    
    /**
     * 执行Saga事务 - 失败场景（库存不足）
     * 使用@GlobalTransactional注解管理全局事务
     */
    @GlobalTransactional(rollbackFor = Exception.class)
    public void executeSagaFailure(String orderId, String userId, String productId, int amount, int quantity) {
        System.out.println("=== 开始执行Saga事务（失败场景 - 库存不足） ===");
        
        try {
            // 步骤1：创建订单
            createOrder.commit(orderId, userId, amount, null);

            // 步骤2：扣减库存
            deductInventory.commit(productId, quantity, null);

            // 步骤3：扣减账户余额
            deductAccount.commit(userId, amount, null);

            System.out.println("=== Saga事务执行成功 ===");
            
        } catch (Exception e) {
            System.out.println("=== Saga事务执行失败，Seata将自动执行补偿 ===");
            System.out.println("错误信息: " + e.getMessage());
            throw e; // 重新抛出异常，让Seata处理补偿
        }
    }
    
    /**
     * 简单的Saga事务示例
     * 演示最基本的Saga使用
     */
    @GlobalTransactional(rollbackFor = Exception.class)
    public void simpleSagaExample(String orderId, String userId, int amount) {
        System.out.println("=== 简单Saga示例 ===");
        
        try {
            // 创建订单
            createOrder.commit(orderId, userId, amount, null);
            
            // 扣减账户余额
            deductAccount.commit(userId, amount, null);
            
            System.out.println("=== 简单Saga示例完成 ===");
            
        } catch (Exception e) {
            System.out.println("简单Saga事务执行异常: " + e.getMessage());
            throw e;
        }
    }
}