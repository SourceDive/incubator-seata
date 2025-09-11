# Seata 分布式事务测试程序

这是一个包含两个分支事务的简单Seata测试程序，用于演示分布式事务的工作原理。

## 项目结构

```
simple_at_demo_01/
├── config/
│   └── TestConfig.java          # 配置两个数据源
├── service/
│   ├── AccountService.java      # 账户服务（操作账户数据库）
│   ├── InventoryService.java    # 库存服务（操作库存数据库）
│   └── OrderService.java        # 订单协调服务（调用两个分支事务）
├── ApplicationTest.java         # 原有的单数据源测试
├── DistributedTransactionTest.java # 新的分布式事务测试
├── setup_database.sql           # 数据库设置脚本
├── run_test.sh                  # 测试运行脚本
├── seata-config.md              # Seata配置说明
└── README.md                    # 说明文档
```

## 分布式事务架构

### 数据源配置
- **账户数据源**: `seata_test_20250804` 数据库
- **库存数据源**: `seata_inventory_test` 数据库
- 每个数据源都用 `DataSourceProxy` 包装，启用Seata AT模式

### 服务层设计
1. **AccountService**: 处理账户相关操作（扣款、转账）
2. **InventoryService**: 处理库存相关操作（减少库存、增加库存）
3. **OrderService**: 协调服务，调用两个分支事务

### 事务流程
```
OrderService.createOrder()
├── 分支事务1: AccountService.transferMoneyCommit() (账户数据库)
└── 分支事务2: InventoryService.decreaseInventory() (库存数据库)
```

## 快速开始

### 1. 数据库设置
```bash
# 执行数据库设置脚本
mysql -u root -pmysql123 < setup_database.sql
```

### 2. 运行测试
```bash
# 使用脚本运行（推荐）
./run_test.sh

# 或手动运行
mvn test -Dtest=DistributedTransactionTest
```

## 测试用例

### DistributedTransactionTest
- `testDistributedTransactionCommit()`: 测试分布式事务提交
- `testDistributedTransactionRollback()`: 测试分布式事务回滚
- `testBranchTransactionXidPropagation()`: 测试XID传播
- `testDataSourceIndependence()`: 测试数据源独立性
- `testDistributedTransactionIsolation()`: 测试事务隔离性

## 关键特性

1. **真正的分布式事务**: 两个不同的数据库，两个分支事务
2. **自动回滚机制**: 任一分支事务失败，整个事务回滚
3. **XID传播**: 全局事务ID在各个分支事务间传播
4. **数据一致性**: 确保两个数据库的数据状态一致

## 前置条件

1. **MySQL服务**: 确保MySQL服务运行
2. **Seata TC服务器**: 用于事务协调（可选，但推荐）
3. **数据库权限**: 创建数据库和表的权限

## 数据库表结构

### 账户表 (account)
```sql
CREATE TABLE account (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    balance INT
);
```

### 库存表 (inventory)
```sql
CREATE TABLE inventory (
    product_id INT PRIMARY KEY,
    product_name VARCHAR(50),
    stock INT
);
```

## 常见问题

### 1. 数据库连接失败
- 检查MySQL服务是否运行
- 验证用户名密码是否正确
- 确认数据库名称是否正确

### 2. 事务不生效
- 确保数据源用 `DataSourceProxy` 包装
- 检查 `@GlobalTransactional` 注解是否正确
- 验证服务是否被Spring正确代理

### 3. 测试失败
- 检查数据库表结构是否正确
- 确认初始数据是否正确插入
- 查看测试日志了解具体错误

## 扩展建议

1. **添加更多微服务**: 支付服务、物流服务等
2. **实现TCC模式**: 手动控制事务的Try-Confirm-Cancel
3. **集成Spring Cloud**: 真正的微服务架构测试
4. **性能测试**: 测试高并发下的分布式事务性能

## 学习要点

这个程序演示了Seata分布式事务的核心概念：
- **全局事务**: 跨多个数据源的事务
- **分支事务**: 每个数据源上的本地事务
- **事务协调**: TC服务器协调所有分支事务
- **数据一致性**: 确保分布式环境下的数据一致性
