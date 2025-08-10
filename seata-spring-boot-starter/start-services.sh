#!/bin/bash

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 启动 Seata + MySQL 服务${NC}"
echo "=================================="

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

echo -e "${YELLOW}📦 拉取最新镜像...${NC}"
docker-compose pull

echo -e "${YELLOW}🏗️  构建并启动服务...${NC}"
docker-compose up -d

# 等待服务启动
echo -e "${YELLOW}⏳ 等待服务启动...${NC}"
echo "正在等待 MySQL 启动..."

# 等待 MySQL 健康检查通过
for i in {1..30}; do
    if docker-compose ps mysql | grep -q "healthy"; then
        echo -e "${GREEN}✅ MySQL 已启动并就绪${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

echo "正在等待 Seata Server 启动..."

# 等待 Seata Server 健康检查通过
for i in {1..30}; do
    if docker-compose ps seata-server | grep -q "healthy"; then
        echo -e "${GREEN}✅ Seata Server 已启动并就绪${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

echo ""
echo -e "${GREEN}🎉 所有服务已启动完成！${NC}"
echo "=================================="

# 显示服务状态
echo -e "${BLUE}📊 服务状态：${NC}"
docker-compose ps

echo ""
echo -e "${BLUE}📝 连接信息：${NC}"
echo "MySQL:"
echo "  - Host: localhost"
echo "  - Port: 3306"
echo "  - Database: seata_test_20250804"
echo "  - Username: root"
echo "  - Password: mysql123"
echo ""
echo "Seata Server:"
echo "  - Host: localhost"
echo "  - Port: 8091"
echo "  - Console: http://localhost:7091"
echo ""

echo -e "${BLUE}📋 常用命令：${NC}"
echo "  查看日志: docker-compose logs -f [service_name]"
echo "  停止服务: docker-compose down"
echo "  重启服务: docker-compose restart"
echo "  查看状态: docker-compose ps"
echo ""

echo -e "${GREEN}✨ 现在可以运行测试了！${NC}"

