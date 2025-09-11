-- Seata TCC 模式测试数据库初始化脚本
-- 数据库名称: seata_tcct_20250911

-- 创建数据库
CREATE DATABASE IF NOT EXISTS seata_tcct_20250911 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE seata_tcct_20250911;

-- 创建账户表
CREATE TABLE IF NOT EXISTS account (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    balance INT NOT NULL DEFAULT 0,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建 TCC 记录表（用于存储 TCC 状态）
CREATE TABLE IF NOT EXISTS tcc_record (
    id VARCHAR(64) PRIMARY KEY,
    xid VARCHAR(128) NOT NULL,
    branch_id BIGINT NOT NULL,
    account_id VARCHAR(32) NOT NULL,
    amount INT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'TRY', -- TRY, CONFIRM, CANCEL
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_xid (xid),
    INDEX idx_account_id (account_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入测试数据
INSERT INTO account (id, name, balance) VALUES 
(1, 'UserA', 1000),
(2, 'UserB', 1000),
(3, 'UserC', 500),
(0, 'System', 10000)
ON DUPLICATE KEY UPDATE 
    name = VALUES(name),
    balance = VALUES(balance);

-- 显示创建的表结构
SHOW TABLES;

-- 显示账户表数据
SELECT * FROM account;

-- 显示 TCC 记录表结构
DESCRIBE tcc_record;
