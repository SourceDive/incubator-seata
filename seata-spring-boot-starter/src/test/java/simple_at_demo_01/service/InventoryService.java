package simple_at_demo_01.service;

import org.apache.seata.core.context.RootContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final JdbcTemplate inventoryJdbcTemplate;

    public InventoryService(@Qualifier("inventoryJdbcTemplate") JdbcTemplate inventoryJdbcTemplate) {
        this.inventoryJdbcTemplate = inventoryJdbcTemplate;
    }

    /**
     * 减少库存
     */
    public void decreaseInventory(int productId, int quantity) {
        System.out.println("库存服务 - 减少库存，产品ID: " + productId + ", 数量: " + quantity);
        System.out.println("库存服务 - XID: " + RootContext.getXID());
        
        // 检查库存是否足够
        Integer currentStock = inventoryJdbcTemplate.queryForObject(
            "SELECT stock FROM inventory WHERE product_id = ?", 
            Integer.class, 
            productId
        );
        
        if (currentStock == null || currentStock < quantity) {
            throw new RuntimeException("库存不足，产品ID: " + productId + 
                "，当前库存: " + currentStock + "，需要: " + quantity);
        }
        
        // 减少库存
        int updatedRows = inventoryJdbcTemplate.update(
            "UPDATE inventory SET stock = stock - ? WHERE product_id = ?", 
            quantity, 
            productId
        );
        
        if (updatedRows == 0) {
            throw new RuntimeException("更新库存失败，产品ID: " + productId);
        }
        
        System.out.println("库存服务 - 库存减少成功");
    }

    /**
     * 增加库存
     */
    public void increaseInventory(int productId, int quantity) {
        System.out.println("库存服务 - 增加库存，产品ID: " + productId + ", 数量: " + quantity);
        System.out.println("库存服务 - XID: " + RootContext.getXID());
        
        int updatedRows = inventoryJdbcTemplate.update(
            "UPDATE inventory SET stock = stock + ? WHERE product_id = ?", 
            quantity, 
            productId
        );
        
        if (updatedRows == 0) {
            throw new RuntimeException("更新库存失败，产品ID: " + productId);
        }
        
        System.out.println("库存服务 - 库存增加成功");
    }

    /**
     * 获取库存
     */
    public int getStock(int productId) {
        Integer stock = inventoryJdbcTemplate.queryForObject(
            "SELECT stock FROM inventory WHERE product_id = ?", 
            Integer.class, 
            productId
        );
        return stock != null ? stock : 0;
    }
}
