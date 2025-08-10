#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}📊 Seata + MySQL 服务状态${NC}"
echo "=================================="

# 检查 docker-compose.yml 是否存在
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}❌ docker-compose.yml 文件不存在${NC}"
    exit 1
fi

echo -e "${YELLOW}📦 容器状态：${NC}"
docker-compose ps

echo ""
echo -e "${YELLOW}💾 数据卷状态：${NC}"
echo "MySQL 数据卷："
docker volume ls | grep seata_mysql
echo "Seata 数据卷："
docker volume ls | grep seata_server

echo ""
echo -e "${YELLOW}🌐 网络状态：${NC}"
docker network ls | grep seata

echo ""
echo -e "${YELLOW}📋 快速命令：${NC}"
echo "  查看 MySQL 日志:     docker-compose logs -f mysql"
echo "  查看 Seata 日志:     docker-compose logs -f seata-server"
echo "  进入 MySQL 容器:     docker-compose exec mysql mysql -uroot -pmysql123"
echo "  重启所有服务:        docker-compose restart"
echo "  停止所有服务:        ./stop-services.sh"

echo ""
# 检查服务健康状态
if docker-compose ps mysql | grep -q "healthy"; then
    echo -e "${GREEN}✅ MySQL 运行正常${NC}"
else
    echo -e "${RED}❌ MySQL 运行异常${NC}"
fi

if docker-compose ps seata-server | grep -q "healthy"; then
    echo -e "${GREEN}✅ Seata Server 运行正常${NC}"
else
    echo -e "${RED}❌ Seata Server 运行异常${NC}"
fi

