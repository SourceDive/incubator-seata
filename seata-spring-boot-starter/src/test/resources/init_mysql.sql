-- 创建数据库
CREATE DATABASE IF NOT EXISTS seata_test_20250804 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE seata_test_20250804;

-- 创建账户表
CREATE TABLE IF NOT EXISTS account (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    balance INT NOT NULL DEFAULT 0
);

-- 创建undo_log表（Seata AT模式必需）
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

-- 插入初始数据
-- INSERT INTO account (id, name, balance) VALUES (1, 'UserA', 1000) ON DUPLICATE KEY UPDATE balance = 1000;
-- INSERT INTO account (id, name, balance) VALUES (2, 'UserB', 1000) ON DUPLICATE KEY UPDATE balance = 1000;