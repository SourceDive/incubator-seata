# Seata TCC 模式学习指南

## 什么是 TCC 模式？

TCC (Try-Confirm-Cancel) 是分布式事务的一种实现模式，它将分布式事务分为三个阶段：

1. **Try**: 尝试执行，预留资源
2. **Confirm**: 确认执行，提交资源
3. **Cancel**: 取消执行，释放资源

## TCC 模式的核心思想

### 1. 两阶段提交的变种
- **第一阶段（Try）**: 只做资源预留，不真正执行业务
- **第二阶段（Confirm/Cancel）**: 根据第一阶段的结果决定提交或回滚

### 2. 业务补偿
- 通过业务逻辑实现事务的补偿
- 每个操作都有对应的补偿操作

## 本案例的业务场景

### 账户扣款场景
- **Try**: 冻结账户金额（预留资源）
- **Confirm**: 真正扣款（提交资源）
- **Cancel**: 解冻金额（释放资源）

## 代码结构解析

### 1. TCC 接口定义
```java
@LocalTCC
public interface AccountTccService {
    @TwoPhaseBusinessAction(
        name = "accountTcc", 
        commitMethod = "confirm", 
        rollbackMethod = "cancel"
    )
    boolean tryDeduct(String accountId, int amount);
    
    boolean confirm(BusinessActionContext context);
    boolean cancel(BusinessActionContext context);
}
```

**关键注解说明：**
- `@LocalTCC`: 标识这是一个 TCC 接口
- `@TwoPhaseBusinessAction`: 定义两阶段业务动作
  - `name`: 业务动作名称
  - `commitMethod`: 确认方法名
  - `rollbackMethod`: 回滚方法名

### 2. TCC 实现类
```java
@Service
public class AccountTccServiceImpl implements AccountTccService {
    
    // Try 阶段：冻结金额
    public boolean tryDeduct(String accountId, int amount) {
        // 1. 检查余额是否足够
        // 2. 冻结金额（预留资源）
        // 3. 记录冻结日志
    }
    
    // Confirm 阶段：真正扣款
    public boolean confirm(BusinessActionContext context) {
        // 1. 获取 Try 阶段的参数
        // 2. 真正扣款
        // 3. 清理冻结记录
    }
    
    // Cancel 阶段：解冻金额
    public boolean cancel(BusinessActionContext context) {
        // 1. 获取 Try 阶段的参数
        // 2. 解冻金额
        // 3. 清理冻结记录
    }
}
```

## TCC 模式的关键特性

### 1. 幂等性
- 每个方法都可能被多次调用
- 必须保证多次调用的结果一致

### 2. 空回滚
- 当 Try 阶段失败时，Cancel 阶段可能被调用
- Cancel 方法需要处理 Try 阶段未执行的情况

### 3. 悬挂
- 当网络异常时，可能出现 Try 阶段在 Cancel 阶段之后执行
- 需要避免这种情况

### 4. 业务补偿
- 通过业务逻辑实现事务的补偿
- 每个操作都有对应的补偿操作

## 测试场景说明

### 1. 成功场景
```java
@Test
@GlobalTransactional
void testTccSuccess() {
    // 执行 TCC 扣款
    boolean result = accountTccService.tryDeduct("1", 100);
    assertTrue(result, "Try 阶段应该成功");
    
    // 验证冻结金额
    int frozenAmount = accountTccService.getFrozenAmount("1");
    assertEquals(100, frozenAmount, "冻结金额应该是 100");
}
```

### 2. 回滚场景
```java
@Test
@GlobalTransactional
void testTccRollback() {
    try {
        // 执行 TCC 扣款
        accountTccService.tryDeduct("2", 100);
        
        // 模拟业务异常，触发回滚
        throw new RuntimeException("模拟业务异常");
    } catch (RuntimeException e) {
        // 验证回滚后冻结金额被清理
        int frozenAmount = accountTccService.getFrozenAmount("2");
        assertEquals(0, frozenAmount, "回滚后冻结金额应该为 0");
    }
}
```

## 与 AT 模式的对比

| 特性 | AT 模式 | TCC 模式 |
|------|---------|----------|
| **实现方式** | 自动生成 UNDO_LOG | 手动实现 Try-Confirm-Cancel |
| **性能** | 较高（自动生成） | 较低（手动实现） |
| **复杂度** | 简单（框架自动处理） | 复杂（需要手动实现） |
| **适用场景** | 大部分 CRUD 操作 | 需要精确控制的业务 |
| **资源占用** | 较少 | 较多 |
| **学习成本** | 低 | 高 |

## 最佳实践

### 1. 幂等性设计
```java
// 使用唯一标识确保幂等性
public boolean tryDeduct(String accountId, int amount) {
    String lockKey = "lock:" + accountId + ":" + amount;
    if (isLocked(lockKey)) {
        return true; // 已经处理过，直接返回成功
    }
    // 执行业务逻辑
}
```

### 2. 异常处理
```java
public boolean confirm(BusinessActionContext context) {
    try {
        // 业务逻辑
        return true;
    } catch (Exception e) {
        // 记录日志
        log.error("Confirm 阶段失败", e);
        return false;
    }
}
```

### 3. 状态管理
```java
// 使用数据库表记录 TCC 状态
CREATE TABLE tcc_record (
    id VARCHAR(64) PRIMARY KEY,
    account_id VARCHAR(32),
    amount INT,
    status VARCHAR(16), -- TRY, CONFIRM, CANCEL
    created_time TIMESTAMP
);
```

## 常见问题

### 1. 空回滚问题
**问题**: Try 阶段失败，但 Cancel 阶段被调用
**解决**: 在 Cancel 方法中检查 Try 阶段是否执行过

### 2. 悬挂问题
**问题**: Try 阶段在 Cancel 阶段之后执行
**解决**: 在 Try 方法中检查 Cancel 阶段是否已经执行

### 3. 幂等性问题
**问题**: 同一操作被多次执行
**解决**: 使用唯一标识和状态检查

## 学习建议

1. **先理解概念**: 理解 TCC 模式的核心思想
2. **动手实践**: 运行本案例的测试代码
3. **观察日志**: 观察 Try-Confirm-Cancel 的执行过程
4. **修改代码**: 尝试修改业务逻辑，观察结果
5. **扩展功能**: 添加更多的 TCC 服务

## 下一步学习

1. 学习 TCC 模式的高级特性
2. 了解 TCC 模式在微服务架构中的应用
3. 学习 TCC 模式的性能优化
4. 了解 TCC 模式的最佳实践
