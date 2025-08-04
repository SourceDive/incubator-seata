#!/bin/bash

echo "正在启动本地构建的Seata TC服务器..."

# 检查是否有构建好的Seata服务器
if [ ! -d "../distribution/target" ]; then
    echo "错误：没有找到构建好的Seata服务器"
    echo "请先构建Seata项目："
    echo "cd .. && mvn -Prelease-seata -Dmaven.test.skip=true clean install -U"
    exit 1
fi

# 查找构建好的Seata服务器目录
SEATA_SERVER_DIR=$(find ../distribution/target -name "apache-seata-*-bin" -type d | head -1)

if [ -z "$SEATA_SERVER_DIR" ]; then
    echo "错误：没有找到构建好的Seata服务器目录"
    exit 1
fi

echo "找到Seata服务器目录: $SEATA_SERVER_DIR"

# 复制配置文件
cp seata-server-config/application.yml "$SEATA_SERVER_DIR/seata-server/resources/"

# 启动Seata服务器
cd "$SEATA_SERVER_DIR/seata-server"
echo "正在启动Seata服务器..."
./bin/seata-server.sh -p 8091 -h 127.0.0.1 