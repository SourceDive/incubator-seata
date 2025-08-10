#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🛑 停止 Seata + MySQL 服务${NC}"
echo "=================================="

# 检查 docker-compose.yml 是否存在
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}❌ docker-compose.yml 文件不存在${NC}"
    exit 1
fi

echo -e "${YELLOW}📊 当前服务状态：${NC}"
docker-compose ps

echo ""
echo -e "${YELLOW}🛑 正在停止服务...${NC}"
docker-compose down

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 所有服务已停止${NC}"
    echo ""
    echo -e "${BLUE}📋 数据持久化说明：${NC}"
    echo "  - MySQL 数据保存在 Docker 卷中，重启后不会丢失"
    echo "  - Seata 配置和日志也会保持"
    echo ""
    echo -e "${BLUE}💡 重新启动服务：${NC}"
    echo "  ./start-services.sh"
else
    echo -e "${RED}❌ 停止服务时发生错误${NC}"
    exit 1
fi

