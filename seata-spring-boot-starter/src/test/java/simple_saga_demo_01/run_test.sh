#!/bin/bash

# Saga测试运行脚本

echo "=== Seata Saga模式测试 ==="

# 检查数据库连接
echo "1. 检查数据库连接..."
mysql -u root -p -e "SELECT 1" 2>/dev/null
if [ $? -ne 0 ]; then
    echo "错误：无法连接到MySQL数据库"
    echo "请确保MySQL服务正在运行，并且用户名密码正确"
    exit 1
fi

# 初始化数据库
echo "2. 初始化测试数据库..."
mysql -u root -p < setup_database.sql
if [ $? -ne 0 ]; then
    echo "错误：数据库初始化失败"
    exit 1
fi

echo "3. 运行Saga测试..."
# 运行测试
mvn test -Dtest=SimpleSagaTest

echo "=== 测试完成 ==="

