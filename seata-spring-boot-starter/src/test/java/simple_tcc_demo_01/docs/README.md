# Seata TCC 模式简单演示

这是一个最简单的 Seata TCC 模式学习案例，演示 TCC 模式的核心概念。

## TCC 模式简介

TCC (Try-Confirm-Cancel) 是分布式事务的一种实现模式，包含三个阶段：

1. **Try**: 尝试执行，预留资源
2. **Confirm**: 确认执行，提交资源
3. **Cancel**: 取消执行，释放资源

## 项目结构

```
simple_tcc_demo_01/
├── service/
│   ├── AccountTccService.java      # TCC 接口定义
│   └── AccountTccServiceImpl.java  # TCC 实现类
├── config/
│   └── TccTestConfig.java          # 测试配置
├── TccDemoTest.java               # TCC 测试类
└── README.md                      # 说明文档
```

## 核心概念

### 1. TCC 接口
```java
@LocalTCC
public interface AccountTccService {
    @TwoPhaseBusinessAction(name = "accountTcc", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryDeduct(String accountId, int amount);
    
    boolean confirm(BusinessActionContext context);
    boolean cancel(BusinessActionContext context);
}
```

### 2. 业务场景
- **Try**: 冻结账户金额（预留资源）
- **Confirm**: 真正扣款（提交资源）
- **Cancel**: 解冻金额（释放资源）

## 快速开始

### 1. 运行测试
```bash
mvn test -Dtest=TccDemoTest
```

### 2. 测试场景
- `testTccSuccess()`: TCC 成功场景
- `testTccRollback()`: TCC 回滚场景

## 学习要点

1. **Try 阶段**: 只做资源预留，不真正执行业务
2. **Confirm 阶段**: 真正执行业务逻辑
3. **Cancel 阶段**: 释放 Try 阶段预留的资源
4. **幂等性**: 每个方法都要保证幂等性
5. **异常处理**: 合理处理各种异常情况

## 与 AT 模式的区别

| 特性 | AT 模式 | TCC 模式 |
|------|---------|----------|
| 实现方式 | 自动生成 UNDO_LOG | 手动实现 Try-Confirm-Cancel |
| 性能 | 较高 | 较低 |
| 复杂度 | 简单 | 复杂 |
| 适用场景 | 大部分场景 | 需要精确控制的场景 |
