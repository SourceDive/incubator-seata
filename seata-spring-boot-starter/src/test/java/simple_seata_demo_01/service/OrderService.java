package simple_seata_demo_01.service;

import org.apache.seata.core.context.RootContext;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 协调两个分支事务。
 */
@Service
public class OrderService {

    @Autowired
    private AccountService accountService;
    
    @Autowired
    private InventoryService inventoryService;

    /**
     * 创建订单 - 真正的分布式事务
     * 1. 从账户扣款
     * 2. 减少库存
     * 这两个操作要么都成功，要么都回滚
     */
    @GlobalTransactional
    public void createOrder(int userId, int productId, int quantity, int price) {
        System.out.println("=== 开始创建订单 ===");
        System.out.println("订单服务 - XID: " + RootContext.getXID());
        System.out.println("用户ID: " + userId + ", 产品ID: " + productId + 
            ", 数量: " + quantity + ", 单价: " + price);
        
        int totalAmount = quantity * price;
        
        try {
            // 分支事务1：从账户扣款
            System.out.println("执行分支事务1：账户扣款");
            accountService.transferMoneyCommitNoTx(userId, 0, totalAmount); // 0表示系统账户
            
            // 分支事务2：减少库存
            System.out.println("执行分支事务2：减少库存");
            inventoryService.decreaseInventory(productId, quantity);
            
            System.out.println("✅ 订单创建成功！");
            
        } catch (Exception e) {
            System.out.println("❌ 订单创建失败: " + e.getMessage());
            throw e; // 重新抛出异常，触发全局事务回滚
        }
    }

    /**
     * 取消订单 - 分布式事务回滚测试
     * 1. 从账户扣款（会成功）
     * 2. 减少库存（会失败，触发回滚）
     */
    @GlobalTransactional
    @Transactional
    public void createOrderWithRollback(int userId, int productId, int quantity, int price) {
        System.out.println("=== 开始创建订单（会回滚） ===");
        System.out.println("订单服务 - XID: " + RootContext.getXID());
        System.out.println("用户ID: " + userId + ", 产品ID: " + productId + 
            ", 数量: " + quantity + ", 单价: " + price);
        
        int totalAmount = quantity * price;
        
        try {
            // 分支事务1：从账户扣款
            System.out.println("执行分支事务1：账户扣款");
            accountService.transferMoneyCommitNoTx(userId, 0, totalAmount);
            
            // 分支事务2：减少库存（这里会失败，触发回滚）
            System.out.println("执行分支事务2：减少库存（会失败）");
            inventoryService.decreaseInventory(productId, quantity + 1000); // 故意传入过大的数量
            
        } catch (Exception e) {
            System.out.println("❌ 订单创建失败，触发回滚: " + e.getMessage());
            throw e; // 重新抛出异常，触发全局事务回滚
        }
    }

    /**
     * 获取订单信息
     */
    public void getOrderInfo(int userId, int productId) {
        System.out.println("=== 获取订单信息 ===");
        
        // 获取账户余额
        // 这里需要调用AccountService的方法，暂时用简单的方式
        System.out.println("用户ID: " + userId + ", 产品ID: " + productId);
        
        // 获取库存信息
        int stock = inventoryService.getStock(productId);
        System.out.println("当前库存: " + stock);
    }
}
