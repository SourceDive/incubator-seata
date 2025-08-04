#!/bin/bash

# 使用Docker启动Seata TC服务器
echo "正在使用Docker启动Seata TC服务器..."

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

# 构建Seata服务器镜像
echo "正在构建Seata服务器Docker镜像..."
cd ../distribution

# 检查是否有构建好的target目录
if [ ! -d "target" ]; then
    echo "错误：没有找到target目录，请先构建Seata项目"
    echo "请运行：mvn -Prelease-seata -Dmaven.test.skip=true clean install -U"
    exit 1
fi

# 构建Docker镜像
docker build --no-cache --build-arg SEATA_VERSION=2.4.0 -t seata-server:2.4.0-dev -f docker/server/Dockerfile .

if [ $? -ne 0 ]; then
    echo "错误：Docker镜像构建失败"
    exit 1
fi

# 启动Seata TC服务器容器
echo "正在启动Seata TC服务器容器..."
docker run --name=seata-server \
    -d \
    -p 8091:8091 \
    -p 7091:7091 \
    -e SEATA_PORT=8091 \
    -e SEATA_HOST=0.0.0.0 \
    seata-server:2.4.0-dev

if [ $? -eq 0 ]; then
    echo "✅ Seata TC服务器已成功启动在Docker容器中"
    echo "📊 容器信息："
    docker ps | grep seata-server
    echo ""
    echo "🔍 查看日志：docker logs seata-server"
    echo "🛑 停止服务器：docker stop seata-server"
    echo "🗑️  删除容器：docker rm seata-server"
else
    echo "❌ Seata TC服务器启动失败"
    exit 1
fi

# 回到原目录
cd ../seata-spring-boot-starter 