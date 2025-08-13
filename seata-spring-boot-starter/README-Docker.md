# Docker 服务管理指南

本项目提供了完整的 Docker Compose 配置和智能容器管理脚本，用于快速启动 MySQL 和 Seata 服务。

## 📋 文件说明

### 配置文件
- `docker-compose.yml` - Docker Compose 配置文件
- `seata-server-config/` - Seata 服务器配置目录

### 管理脚本
- `manage-containers.sh` - **推荐使用** - 智能容器管理脚本
- `start-services.sh` - 启动服务脚本
- `stop-services.sh` - 停止服务脚本  
- `status-services.sh` - 查看服务状态脚本

## 🚀 快速开始

### 方法一：使用智能管理脚本（推荐）

```bash
# 给脚本添加执行权限
chmod +x *.sh

# 启动服务（会自动检测现有容器并复用）
./manage-containers.sh
```

### 方法二：使用 Docker Compose 命令

```bash
# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 🔧 智能容器管理特性

`manage-containers.sh` 脚本具有以下智能特性：

### ✅ 容器检测和复用
- 自动检测本地是否已存在对应的容器
- 如果容器存在且停止，会直接启动现有容器
- 如果容器不存在，才会创建新容器
- 避免重复创建容器，保护现有数据

### ✅ 状态监控
- 实时显示容器运行状态
- 健康检查确保服务完全就绪
- 详细的启动过程反馈

### ✅ 错误处理
- 检查 Docker 是否运行
- 验证配置文件是否存在
- 优雅的错误提示和处理

## 📊 服务信息

### MySQL 数据库
- **容器名**: `seata-mysql`
- **端口**: 3306
- **数据库**: `seata_test_20250804`
- **用户名**: `root`
- **密码**: `mysql123`
- **数据持久化**: ✅ 保存在 Docker 卷中

### Seata 事务协调器
- **容器名**: `seata-server-20250810`
- **端口**: 8091 (服务端口), 7091 (控制台端口)
- **版本**: 1.7.1
- **配置**: 使用 `seata-server-config/` 目录下的配置
- **数据持久化**: ✅ 会话和日志保存在 Docker 卷中

## 📝 常用命令

### 查看服务状态
```bash
./status-services.sh
# 或
docker-compose ps
```

### 查看日志
```bash
# 查看 MySQL 日志
docker-compose logs -f mysql

# 查看 Seata 日志
docker-compose logs -f seata-server

# 查看所有服务日志
docker-compose logs -f
```

### 进入容器
```bash
# 进入 MySQL 容器
docker-compose exec mysql mysql -uroot -pmysql123

# 进入 Seata 容器
docker-compose exec seata-server sh
```

### 重启服务
```bash
# 重启所有服务
docker-compose restart

# 重启特定服务
docker-compose restart mysql
docker-compose restart seata-server
```

### 停止服务
```bash
./stop-services.sh
# 或
docker-compose down
```

## 🔄 数据持久化

### MySQL 数据
- 数据存储在 Docker 卷 `seata_mysql_data` 中
- 重启容器后数据不会丢失
- 删除容器后数据仍然保留

### Seata 数据
- 会话数据存储在 Docker 卷 `seata_server_data` 中
- 日志存储在 Docker 卷 `seata_server_logs` 中
- 配置通过 `seata-server-config/` 目录挂载

## 🛠️ 故障排除

### 容器启动失败
```bash
# 查看详细错误信息
docker-compose logs

# 重新创建容器
docker-compose down
docker-compose up -d
```

### 端口冲突
如果端口被占用，可以修改 `docker-compose.yml` 中的端口映射：
```yaml
ports:
  - "3307:3306"  # 改为其他端口
```

### 数据卷问题
```bash
# 查看数据卷
docker volume ls | grep seata

# 清理数据卷（谨慎操作，会删除所有数据）
docker-compose down -v
```

## 📚 测试连接

服务启动后，可以使用以下方式测试连接：

### 测试 MySQL 连接
```bash
mysql -h localhost -P 3306 -u root -pmysql123 seata_test_20250804
```

### 测试 Seata 连接
```bash
# 检查 Seata 服务端口
telnet localhost 8091

# 访问 Seata 控制台
open http://localhost:7091
```

## 🎯 最佳实践

1. **首次使用**: 运行 `./manage-containers.sh` 创建并启动服务
2. **日常使用**: 直接运行 `./manage-containers.sh` 复用现有容器
3. **开发调试**: 使用 `./status-services.sh` 查看服务状态
4. **问题排查**: 使用 `docker-compose logs` 查看详细日志
5. **数据备份**: 定期备份 Docker 卷中的数据

## 🔗 相关链接

- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [Seata 官方文档](https://seata.io/zh-cn/)
- [MySQL Docker 镜像](https://hub.docker.com/_/mysql)
