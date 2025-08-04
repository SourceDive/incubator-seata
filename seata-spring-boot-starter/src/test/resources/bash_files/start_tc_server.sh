#!/bin/bash

# 启动Seata TC服务器
echo "正在启动Seata TC服务器..."

# 切换到distribution目录
cd ../distribution

# 启动TC服务器
./bin/seata-server.sh -p 8091 -h 127.0.0.1

echo "Seata TC服务器已启动在端口8091" 