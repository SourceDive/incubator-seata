#!/bin/bash

echo "正在启动MySQL容器（带数据持久化）..."

# 创建数据卷目录
echo "创建数据卷目录..."
mkdir -p ~/mysql_data
mkdir -p ~/mysql_config

docker rm mysql

# 停止并删除现有MySQL容器
#if docker ps | grep -q mysql; then
#    echo "停止现有MySQL容器..."
#    docker stop mysql
#fi

# 启动MySQL容器，使用数据持久化
docker run --name=mysql \
    -d \
    -p 3306:3306 \
    -v ~/mysql_data:/var/lib/mysql \
    -v ~/mysql_config:/etc/mysql/conf.d \
    -e MYSQL_ROOT_PASSWORD=mysql123 \
    -e MYSQL_DATABASE=seata_test_20250804 \
    -e MYSQL_PASSWORD=mysql123 \
    -e MYSQL_ROOT_HOST=% \
    swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/mysql:8.0.39-linuxarm64

if [ $? -eq 0 ]; then
    echo "✅ MySQL容器已启动（数据持久化）"
    echo "📊 容器状态："
    docker ps | grep mysql
    echo ""
    echo "📁 数据卷位置："
    echo "数据目录: ~/mysql_data"
    echo "配置目录: ~/mysql_config"
    echo ""
    echo "⏳ 等待MySQL启动..."
    sleep 30
    echo "🔍 查看日志：docker logs mysql"
    echo ""
    echo "📝 连接信息："
    echo "Host: localhost"
    echo "Port: 3306"
    echo "Database: seata_test_20250804"
    echo "User: root"
    echo "Password: mysql123"
    echo ""
    echo "💾 数据持久化说明："
    echo "- 数据存储在 ~/mysql_data"
    echo "- 重启容器后数据不会丢失"
    echo "- 删除容器后数据仍然保留"
else
    echo "❌ MySQL启动失败"
    exit 1
fi 