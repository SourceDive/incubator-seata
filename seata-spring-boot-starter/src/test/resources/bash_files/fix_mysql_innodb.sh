#!/bin/bash

echo "正在修复MySQL InnoDB配置..."

# 检查MySQL容器状态
if docker ps | grep -q mysql; then
    echo "MySQL容器正在运行，尝试重启..."
    docker restart mysql
    sleep 10
    echo "MySQL已重启"
else
    echo "MySQL容器未运行，请先启动MySQL"
    exit 1
fi

# 等待MySQL完全启动
echo "等待MySQL完全启动..."
sleep 30

# 检查MySQL连接
echo "检查MySQL连接..."
docker exec mysql mysql -uroot -pmysql123 -e "SELECT 1;" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "✅ MySQL连接正常"
    
    # 创建数据库和表
    echo "创建数据库和表..."
    docker exec mysql mysql -uroot -pmysql123 -e "
    CREATE DATABASE IF NOT EXISTS seata_test_20250804 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    USE seata_test_20250804;
    
    CREATE TABLE IF NOT EXISTS account (
        id INT PRIMARY KEY,
        name VARCHAR(50) NOT NULL,
        balance INT NOT NULL DEFAULT 0
    );
    
    CREATE TABLE IF NOT EXISTS undo_log (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        branch_id BIGINT NOT NULL,
        xid VARCHAR(100) NOT NULL,
        context VARCHAR(128) NOT NULL,
        rollback_info LONGBLOB NOT NULL,
        log_status INT NOT NULL,
        log_created DATETIME NOT NULL,
        log_modified DATETIME NOT NULL,
        UNIQUE KEY ux_undo_log (xid, branch_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    
    INSERT INTO account (id, name, balance) VALUES (1, 'UserA', 1000) ON DUPLICATE KEY UPDATE balance = 1000;
    INSERT INTO account (id, name, balance) VALUES (2, 'UserB', 1000) ON DUPLICATE KEY UPDATE balance = 1000;
    "
    
    echo "✅ 数据库和表创建完成"
else
    echo "❌ MySQL连接失败"
    exit 1
fi 