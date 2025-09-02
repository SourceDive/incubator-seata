#!/bin/bash

# Seata 分布式事务测试运行脚本

echo "=== Seata 分布式事务测试程序 ==="
echo ""

# 检查MySQL连接
echo "1. 检查MySQL连接..."
if mysql -u root -pmysql123 -e "SELECT 1" > /dev/null 2>&1; then
    echo "✅ MySQL连接正常"
else
    echo "❌ MySQL连接失败，请检查MySQL服务是否运行"
    exit 1
fi

# 设置数据库
echo ""
echo "2. 设置数据库..."
mysql -u root -pmysql123 < setup_database.sql
if [ $? -eq 0 ]; then
    echo "✅ 数据库设置完成"
else
    echo "❌ 数据库设置失败"
    exit 1
fi

# 运行测试
echo ""
echo "3. 运行分布式事务测试..."
echo "   注意：确保Seata TC服务器正在运行"
echo ""

# 运行分布式事务测试
echo "运行 DistributedTransactionTest..."
mvn test -Dtest=DistributedTransactionTest

echo ""
echo "4. 测试完成！"
echo ""
echo "如果看到错误，请检查："
echo "1. MySQL服务是否运行"
echo "2. 数据库连接配置是否正确"
echo "3. Seata TC服务器是否运行"
echo "4. 表结构是否正确创建"
