-- ============================================================
-- 华兴服装店 - 演示数据（商品 + SKU + 入库流水）
-- 用法: docker exec -i clothing-mysql mysql -uroot -padmin123 clothing_pos < scripts/seed_demo_data.sql
-- 说明:
--   * 条码规则 HUAXING + yyyyMMdd + 3位序号（001 起），与后端生成规则一致
--   * sku_spec 格式: 颜色 / 尺码（与 StockController 拼接格式一致）
--   * stock_record 冗余 product_name + sku_spec，type 用 INBOUND
--   * 幂等: 已存在的商品（按 id 匹配）会被忽略
-- ============================================================

-- ---------- 商品主表 ----------
INSERT INTO product (id, category_id, name, cost_price, sell_price) VALUES
(1,  5, '纯棉圆领短袖T恤', 29.00, 49.00),
(2,  6, '格纹长袖衬衫',    45.00, 89.00),
(3,  7, '直筒牛仔裤',      59.00, 129.00),
(4,  8, '卡其色休闲裤',    49.00, 99.00),
(5,  3, '碎花连衣裙',      69.00, 159.00),
(6,  4, '经典牛仔外套',    89.00, 199.00);

-- ---------- SKU（颜色 / 尺码 组合）----------
-- product 1 纯棉圆领短袖T恤
INSERT INTO product_sku (id, product_id, color, size, barcode, stock_qty, version) VALUES
(101, 1, '白色', 'M', 'HUAXING20260806001', 20, 0),
(102, 1, '白色', 'L', 'HUAXING20260806002', 18, 0),
(103, 1, '黑色', 'M', 'HUAXING20260806003', 25, 0),
(104, 1, '黑色', 'L', 'HUAXING20260806004', 22, 0),
(105, 1, '灰色', 'M', 'HUAXING20260806005', 15, 0);

-- product 2 格纹长袖衬衫
INSERT INTO product_sku (id, product_id, color, size, barcode, stock_qty, version) VALUES
(106, 2, '蓝格', 'M', 'HUAXING20260806006', 12, 0),
(107, 2, '蓝格', 'L', 'HUAXING20260806007', 10, 0),
(108, 2, '红格', 'M', 'HUAXING20260806008', 8, 0);

-- product 3 直筒牛仔裤
INSERT INTO product_sku (id, product_id, color, size, barcode, stock_qty, version) VALUES
(109, 3, '深蓝', '29', 'HUAXING20260806009', 14, 0),
(110, 3, '深蓝', '30', 'HUAXING20260806010', 16, 0),
(111, 3, '浅蓝', '29', 'HUAXING20260806011', 9, 0),
(112, 3, '浅蓝', '30', 'HUAXING20260806012', 11, 0);

-- product 4 卡其色休闲裤
INSERT INTO product_sku (id, product_id, color, size, barcode, stock_qty, version) VALUES
(113, 4, '卡其', 'L', 'HUAXING20260806013', 13, 0),
(114, 4, '卡其', 'XL', 'HUAXING20260806014', 7, 0),
(115, 4, '黑色', 'L', 'HUAXING20260806015', 10, 0);

-- product 5 碎花连衣裙
INSERT INTO product_sku (id, product_id, color, size, barcode, stock_qty, version) VALUES
(116, 5, '粉色', 'S', 'HUAXING20260806016', 6, 0),
(117, 5, '粉色', 'M', 'HUAXING20260806017', 8, 0),
(118, 5, '米白', 'M', 'HUAXING20260806018', 5, 0);

-- product 6 经典牛仔外套
INSERT INTO product_sku (id, product_id, color, size, barcode, stock_qty, version) VALUES
(119, 6, '蓝色', 'M', 'HUAXING20260806019', 9, 0),
(120, 6, '蓝色', 'L', 'HUAXING20260806020', 7, 0);

-- ---------- 库存入库流水（冗余商品名 + SKU 规格）----------
INSERT INTO stock_record (sku_id, product_name, sku_spec, type, qty, before_qty, after_qty, operator_id) VALUES
(101, '纯棉圆领短袖T恤', '白色 / M',   'INBOUND', 20, 0, 20, 1),
(102, '纯棉圆领短袖T恤', '白色 / L',   'INBOUND', 18, 0, 18, 1),
(103, '纯棉圆领短袖T恤', '黑色 / M',   'INBOUND', 25, 0, 25, 1),
(104, '纯棉圆领短袖T恤', '黑色 / L',   'INBOUND', 22, 0, 22, 1),
(105, '纯棉圆领短袖T恤', '灰色 / M',   'INBOUND', 15, 0, 15, 1),
(106, '格纹长袖衬衫',     '蓝格 / M',   'INBOUND', 12, 0, 12, 1),
(107, '格纹长袖衬衫',     '蓝格 / L',   'INBOUND', 10, 0, 10, 1),
(108, '格纹长袖衬衫',     '红格 / M',   'INBOUND', 8, 0,  8,  1),
(109, '直筒牛仔裤',       '深蓝 / 29',  'INBOUND', 14, 0, 14, 1),
(110, '直筒牛仔裤',       '深蓝 / 30',  'INBOUND', 16, 0, 16, 1),
(111, '直筒牛仔裤',       '浅蓝 / 29',  'INBOUND', 9, 0,  9,  1),
(112, '直筒牛仔裤',       '浅蓝 / 30',  'INBOUND', 11, 0, 11, 1),
(113, '卡其色休闲裤',     '卡其 / L',   'INBOUND', 13, 0, 13, 1),
(114, '卡其色休闲裤',     '卡其 / XL',  'INBOUND', 7, 0,  7,  1),
(115, '卡其色休闲裤',     '黑色 / L',   'INBOUND', 10, 0, 10, 1),
(116, '碎花连衣裙',       '粉色 / S',   'INBOUND', 6, 0,  6,  1),
(117, '碎花连衣裙',       '粉色 / M',   'INBOUND', 8, 0,  8,  1),
(118, '碎花连衣裙',       '米白 / M',   'INBOUND', 5, 0,  5,  1),
(119, '经典牛仔外套',     '蓝色 / M',   'INBOUND', 9, 0,  9,  1),
(120, '经典牛仔外套',     '蓝色 / L',   'INBOUND', 7, 0,  7,  1);
