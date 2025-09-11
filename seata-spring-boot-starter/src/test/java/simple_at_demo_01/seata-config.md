# Seata 配置说明

## 核心配置

### 1. 数据源配置
```java
@Bean("accountDataSource")
public DataSource accountDataSource() {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
    ds.setUrl("jdbc:mysql://127.0.0.1:3306/seata_test_20250804?...");
    ds.setUsername("root");
    ds.setPassword("mysql123");
    
    // 关键：使用DataSourceProxy包装，启用Seata AT模式
    return new DataSourceProxy(ds, "mysql");
}
```

**重要**: 每个数据源都必须用 `DataSourceProxy` 包装，这样Seata才能拦截SQL语句。

### 2. 全局事务扫描器
```java
@Bean
@Primary
public GlobalTransactionScanner globalTransactionScanner() {
    return new GlobalTransactionScanner("seata-test", "my_test_tx_group");
}
```

- 第一个参数：应用名称
- 第二个参数：事务组名称

### 3. 事务注解
```java
@GlobalTransactional
public void createOrder(int userId, int productId, int quantity, int price) {
    // 分支事务1：账户扣款
    accountService.transferMoneyCommit(userId, 0, totalAmount);
    
    // 分支事务2：库存减少
    inventoryService.decreaseInventory(productId, quantity);
}
```

## 分布式事务流程

### 阶段1：全局事务开始
1. 调用带有 `@GlobalTransactional` 注解的方法
2. Seata自动生成全局事务ID (XID)
3. 将XID绑定到当前线程的RootContext

### 阶段2：分支事务注册
1. 第一个分支事务执行（账户扣款）
   - Seata拦截SQL，生成UNDO_LOG
   - 向TC注册分支事务
2. 第二个分支事务执行（库存减少）
   - 同样生成UNDO_LOG
   - 向TC注册分支事务

### 阶段3：全局事务提交/回滚
1. **成功情况**: TC通知所有分支事务提交
2. **失败情况**: TC通知所有分支事务回滚，使用UNDO_LOG恢复数据

## 关键概念

### 1. XID传播
- XID通过 `RootContext.getXID()` 获取
- 在同一个线程中，所有分支事务共享同一个XID
- 确保事务的全局一致性

### 2. UNDO_LOG
- 每个分支事务执行前，Seata自动生成UNDO_LOG
- 记录数据修改前的状态
- 用于事务回滚时恢复数据

### 3. 分支事务
- 每个数据源的操作都是一个分支事务
- 分支事务可以独立提交或回滚
- 但最终由全局事务协调器决定

## 常见问题

### 1. 数据源未代理
**现象**: 事务不生效
**解决**: 确保所有数据源都用 `DataSourceProxy` 包装

### 2. 服务未被代理
**现象**: `@GlobalTransactional` 注解不生效
**解决**: 确保服务类被Spring正确扫描和代理

### 3. TC服务器未运行
**现象**: 事务无法协调
**解决**: 启动Seata TC服务器

### 4. 数据库表结构不正确
**现象**: UNDO_LOG无法生成
**解决**: 确保表有主键，并且字段类型支持

## 测试建议

1. **先测试单数据源**: 确保基本的Seata配置正确
2. **再测试多数据源**: 验证分布式事务的协调
3. **测试异常场景**: 确保回滚机制正常工作
4. **监控日志**: 观察XID传播和分支事务注册过程
