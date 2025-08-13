#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 容器名称定义
MYSQL_CONTAINER="seata-mysql"
SEATA_CONTAINER="seata-server-20250810"

# 检查容器是否存在（包括停止的容器）
check_container_exists() {
    local container_name=$1
    docker ps -a -q -f name=$container_name | grep -q .
}

# 检查容器是否正在运行
check_container_running() {
    local container_name=$1
    docker ps -q -f name=$container_name | grep -q .
}

# 启动容器
start_container() {
    local container_name=$1
    local service_name=$2
    
    if check_container_exists $container_name; then
        if check_container_running $container_name; then
            echo -e "${GREEN}✅ $service_name 容器已在运行${NC}"
        else
            echo -e "${YELLOW}🔄 启动 $service_name 容器...${NC}"
            docker start $container_name
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✅ $service_name 容器启动成功${NC}"
            else
                echo -e "${RED}❌ $service_name 容器启动失败${NC}"
                return 1
            fi
        fi
    else
        echo -e "${BLUE}📦 $service_name 容器不存在，将创建新容器${NC}"
        return 2
    fi
}

# 显示容器状态
show_container_status() {
    echo -e "${BLUE}📊 容器状态概览：${NC}"
    echo "=================================="
    
    # MySQL 状态
    if check_container_exists $MYSQL_CONTAINER; then
        if check_container_running $MYSQL_CONTAINER; then
            echo -e "${GREEN}✅ MySQL: 运行中${NC}"
        else
            echo -e "${YELLOW}⏸️  MySQL: 已停止${NC}"
        fi
    else
        echo -e "${RED}❌ MySQL: 不存在${NC}"
    fi
    
    # Seata 状态
    if check_container_exists $SEATA_CONTAINER; then
        if check_container_running $SEATA_CONTAINER; then
            echo -e "${GREEN}✅ Seata: 运行中${NC}"
        else
            echo -e "${YELLOW}⏸️  Seata: 已停止${NC}"
        fi
    else
        echo -e "${RED}❌ Seata: 不存在${NC}"
    fi
    
    echo ""
}

# 主函数
main() {
    echo -e "${BLUE}🔧 智能容器管理工具${NC}"
    echo "=================================="
    
    # 显示当前状态
    show_container_status
    
    # 检查 Docker 是否运行
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}❌ Docker 未运行，请先启动 Docker${NC}"
        exit 1
    fi
    
    # 检查 docker-compose.yml 是否存在
    if [ ! -f "docker-compose.yml" ]; then
        echo -e "${RED}❌ docker-compose.yml 文件不存在${NC}"
        exit 1
    fi
    
    echo -e "${YELLOW}🚀 开始启动服务...${NC}"
    
    # 尝试启动 MySQL
    start_container $MYSQL_CONTAINER "MySQL"
    mysql_result=$?
    
    # 尝试启动 Seata
    start_container $SEATA_CONTAINER "Seata"
    seata_result=$?
    
    # 如果有容器不存在，使用 docker-compose 创建
    if [ $mysql_result -eq 2 ] || [ $seata_result -eq 2 ]; then
        echo -e "${YELLOW}📦 拉取最新镜像...${NC}"
        docker-compose pull
        
        echo -e "${YELLOW}🏗️  创建并启动新容器...${NC}"
        docker-compose up -d
    fi
    
    # 等待服务启动
    echo -e "${YELLOW}⏳ 等待服务启动...${NC}"
    
    # 等待 MySQL 启动
    if [ $mysql_result -ne 0 ]; then
        echo "等待 MySQL 启动..."
        for i in {1..30}; do
            if docker-compose ps mysql 2>/dev/null | grep -q "healthy"; then
                echo -e "${GREEN}✅ MySQL 已启动并就绪${NC}"
                break
            fi
            echo -n "."
            sleep 2
        done
    fi
    
    # 等待 Seata 启动
    if [ $seata_result -ne 0 ]; then
        echo "等待 Seata Server 启动..."
        for i in {1..30}; do
            if docker-compose ps seata-server 2>/dev/null | grep -q "healthy"; then
                echo -e "${GREEN}✅ Seata Server 已启动并就绪${NC}"
                break
            fi
            echo -n "."
            sleep 2
        done
    fi
    
    echo ""
    echo -e "${GREEN}🎉 服务启动完成！${NC}"
    echo "=================================="
    
    # 显示最终状态
    show_container_status
    
    echo -e "${BLUE}📝 连接信息：${NC}"
    echo "MySQL: localhost:3306 (root/mysql123)"
    echo "Seata: localhost:8091"
    echo "Seata Console: http://localhost:7091"
    echo ""
    echo -e "${GREEN}✨ 现在可以运行测试了！${NC}"
}

# 执行主函数
main "$@"
