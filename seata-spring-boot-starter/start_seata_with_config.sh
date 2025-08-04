#!/bin/bash

echo "正在启动带配置文件的Seata TC服务器..."

# 停止并删除现有容器
if docker ps | grep -q seata-server; then
    echo "停止现有容器..."
    docker stop seata-server
    docker rm seata-server
fi

# 启动带配置文件挂载的Seata服务器
docker run --name=seata-server \
    -d \
    -p 8091:8091 \
    -p 7091:7091 \
    -v $(pwd)/seata-server-config/application.yml:/seata-server/resources/application.yml \
    -e SEATA_PORT=8091 \
    -e SEATA_HOST=0.0.0.0 \
    seataio/seata-server:1.7.1

if [ $? -eq 0 ]; then
    echo "✅ Seata TC服务器已启动"
    echo "📊 容器状态："
    docker ps | grep seata-server
    echo ""
    echo "⏳ 等待服务器启动..."
    sleep 10
    echo "🔍 查看日志：docker logs seata-server"
else
    echo "❌ 启动失败"
    exit 1
fi 