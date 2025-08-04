#!/bin/bash

# 使用官方Docker镜像启动Seata TC服务器
echo "正在使用官方Docker镜像启动Seata TC服务器..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "错误：Docker未运行，请先启动Docker"
    exit 1
fi

# 检查是否已经有seata-server容器在运行
if docker ps | grep -q seata-server; then
    echo "发现已有seata-server容器在运行，正在停止..."
    docker stop seata-server
    docker rm seata-server
fi

# 拉取官方Seata服务器镜像
echo "正在拉取官方Seata服务器镜像..."
docker pull apache/seata-server:1.7.1

# 启动Seata TC服务器容器
echo "正在启动Seata TC服务器容器..."
docker run --name=seata-server \
    -d \
    -p 8091:8091 \
    -p 7091:7091 \
    -e SEATA_PORT=8091 \
    -e SEATA_HOST=0.0.0.0 \
    apache/seata-server:1.7.1

if [ $? -eq 0 ]; then
    echo "✅ Seata TC服务器已成功启动在Docker容器中"
    echo "📊 容器信息："
    docker ps | grep seata-server
    echo ""
    echo "🔍 查看日志：docker logs seata-server"
    echo "🛑 停止服务器：docker stop seata-server"
    echo "🗑️  删除容器：docker rm seata-server"
    echo ""
    echo "⏳ 等待服务器启动完成..."
    sleep 5
    echo "🚀 服务器已就绪，可以运行测试了！"
else
    echo "❌ Seata TC服务器启动失败"
    exit 1
fi 