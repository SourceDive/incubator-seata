-- Seata 分布式事务测试数据库设置脚本
-- 执行前请确保MySQL服务运行，并且有足够的权限

-- 1. 创建账户数据库
CREATE DATABASE IF NOT EXISTS seata_test_20250804;
USE seata_test_20250804;

-- 创建账户表
CREATE TABLE IF NOT EXISTS account (
    id INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    balance INT NOT NULL DEFAULT 0
);

-- 插入初始账户数据
INSERT INTO account (id, name, balance) VALUES 
(1, 'UserA', 1000),
(2, 'UserB', 1000),
(0, 'System', 10000)
ON DUPLICATE KEY UPDATE 
name = VALUES(name), 
balance = VALUES(balance);

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

-- 2. 创建库存数据库
CREATE DATABASE IF NOT EXISTS seata_inventory_test;
USE seata_inventory_test;

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

-- 创建库存表
CREATE TABLE IF NOT EXISTS inventory (
    product_id INT PRIMARY KEY,
    product_name VARCHAR(50) NOT NULL,
    stock INT NOT NULL DEFAULT 0
);

-- 插入初始库存数据
INSERT INTO inventory (product_id, product_name, stock) VALUES 
(1, 'ProductA', 100),
(2, 'ProductB', 50)
ON DUPLICATE KEY UPDATE 
product_name = VALUES(product_name), 
stock = VALUES(stock);

-- 3. 验证数据
SELECT '账户数据库数据:' as info;
USE seata_test_20250804;
SELECT * FROM account;

SELECT '库存数据库数据:' as info;
USE seata_inventory_test;
SELECT * FROM inventory;

-- 4. 显示数据库信息
SELECT '数据库创建完成！' as status;
SELECT 
    'seata_test_20250804' as database_name,
    '账户服务数据库' as description
UNION ALL
SELECT 
    'seata_inventory_test' as database_name,
    '库存服务数据库' as description;
