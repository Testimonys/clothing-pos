-- ============================================================
-- 华兴服装店 - 数据库表结构
-- Database: clothing_pos
-- ============================================================

-- 商品分类
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT NULL,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'BOSS/CLERK',
    enabled TINYINT(1) DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品主表
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NULL,
    name VARCHAR(200) NOT NULL,
    image_url VARCHAR(500) NULL,
    cost_price DECIMAL(10,2) DEFAULT 0.00,
    sell_price DECIMAL(10,2) DEFAULT 0.00,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SKU 规格
CREATE TABLE IF NOT EXISTS product_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    color VARCHAR(50) DEFAULT '',
    size VARCHAR(50) DEFAULT '',
    barcode VARCHAR(100) NULL UNIQUE,
    stock_qty INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 库存流水
CREATE TABLE IF NOT EXISTS stock_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL DEFAULT '',
    sku_spec VARCHAR(100) NOT NULL DEFAULT '',
    type VARCHAR(20) NOT NULL COMMENT 'INBOUND/OUTBOUND',
    qty INT NOT NULL,
    before_qty INT NOT NULL,
    after_qty INT NOT NULL,
    operator_id BIGINT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sku_id) REFERENCES product_sku(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 销售订单
CREATE TABLE IF NOT EXISTS sys_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(30) NOT NULL UNIQUE,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    receive_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    change_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    pay_method VARCHAR(20) NOT NULL COMMENT 'CASH/WECHAT/ALIPAY',
    cashier_id BIGINT NULL,
    member_id BIGINT NULL,
    create_time DATETIME(0) DEFAULT CURRENT_TIMESTAMP(0),
    FOREIGN KEY (cashier_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单明细
CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL DEFAULT '',
    sku_spec VARCHAR(100) NOT NULL DEFAULT '',
    unit_price DECIMAL(10,2) NOT NULL,
    qty INT NOT NULL,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    sub_total DECIMAL(10,2) NOT NULL,
    barcode VARCHAR(100) DEFAULT '',
    FOREIGN KEY (order_id) REFERENCES sys_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
