# 最简单的Seata Saga模式测试程序（正确注解版本）

这是一个演示Seata Saga模式的最简单示例，使用正确的Seata Saga注解实现分布式事务。

## 项目结构

```
simple_saga_demo_01/
├── config/
│   └── SagaConfig.java          # Saga配置类
├── service/
│   ├── OrderSagaService.java    # Saga服务实现（使用正确注解）
│   └── SagaOrchestrator.java    # Saga编排器（使用@GlobalTransactional）
├── SimpleSagaTest.java          # 测试类
├── setup_database.sql           # 数据库初始化脚本
├── run_test.sh                  # 测试运行脚本
└── README.md                    # 说明文档
```

## Seata Saga正确注解说明

### 核心注解

1. **@CompensationBusinessAction**: 标记Saga补偿操作
   - `name`: Saga bean名称，必须唯一
   - `compensationMethod`: 补偿方法名称
   - `compensationArgsClasses`: 补偿方法参数类型

2. **@GlobalTransactional**: 管理全局事务
   - `rollbackFor`: 指定回滚的异常类型

### 本示例的注解使用

```java
@Service
public class OrderSagaService {
    
    @CompensationBusinessAction(
        name = "createOrder", 
        compensationMethod = "compensateCreateOrder"
    )
    public void createOrder(String orderId, String userId, int amount, BusinessActionContext context) {
        // 创建订单的业务逻辑
        // 保存参数到上下文
        context.setActionContext("orderId", orderId);
    }
    
    public void compensateCreateOrder(BusinessActionContext context) {
        // 从上下文获取参数
        String orderId = (String) context.getActionContext("orderId");
        // 执行补偿逻辑
    }
}
```

## 业务流程

1. **@GlobalTransactional** → 开始全局事务
2. **@CompensationBusinessAction** → 创建订单
3. **@CompensationBusinessAction** → 扣减库存
4. **@CompensationBusinessAction** → 扣减账户余额
5. 事务提交或回滚

当任何步骤失败时，Seata会自动执行对应的补偿操作。

## 关键特点

### 1. 正确的注解使用
- 使用`@CompensationBusinessAction`而不是错误的`@SagaStart`等注解
- 使用`@GlobalTransactional`管理全局事务

### 2. 上下文参数传递
- 使用`BusinessActionContext`传递参数
- 通过`setActionContext`保存参数
- 通过`getActionContext`获取参数

### 3. 补偿方法签名
- 补偿方法必须接受`BusinessActionContext`参数
- 从上下文获取正向操作的参数

## 运行测试

### 1. 数据库准备

```bash
# 执行数据库初始化脚本
mysql -u root -p < setup_database.sql
```

### 2. 运行测试

```bash
# 方式1：使用脚本运行
./run_test.sh

# 方式2：直接运行Maven测试
mvn test -Dtest=SimpleSagaTest
```

## 测试场景

### 成功场景
- 订单创建成功
- 库存扣减成功
- 账户余额扣减成功
- 所有数据保持一致

### 失败场景（库存不足）
- 订单创建成功
- 库存扣减失败（库存不足）
- Seata自动执行补偿操作：
  - 删除订单（compensateCreateOrder）
  - 恢复库存（compensateDeductInventory）
  - 恢复账户余额（compensateDeductAccount）
- 数据回滚到初始状态

## 与之前版本的对比

| 特性 | 错误注解版本 | 正确注解版本 |
|------|-------------|-------------|
| 注解 | @SagaStart, @SagaEnd | @CompensationBusinessAction |
| 事务管理 | 手动 | @GlobalTransactional |
| 参数传递 | 方法参数 | BusinessActionContext |
| 补偿方法 | 手动调用 | 自动调用 |
| 正确性 | ❌ 错误 | ✅ 正确 |

## 注意事项

1. **Seata版本**: 确保使用Seata 2.4.0或更高版本
2. **依赖配置**: 需要添加saga-annotation依赖
3. **上下文使用**: 必须使用BusinessActionContext传递参数
4. **补偿方法**: 补偿方法必须与注解中指定的名称一致

## 扩展说明

在实际项目中，还可以使用：
- **Seata Saga状态机**: 通过JSON配置文件定义复杂的Saga流程
- **Saga编排器**: 使用专门的编排器管理复杂的业务流程
- **消息驱动Saga**: 结合消息队列实现异步Saga

本示例使用正确的Seata Saga注解，是官方推荐的使用方式。