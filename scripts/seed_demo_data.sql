-- ============================================================
-- 华兴服装店 - 演示数据（商品 + SKU + 入库流水）
-- 用法: docker exec -i clothing-mysql mysql -uroot -padmin123 clothing_pos < scripts/seed_demo_data.sql
-- 说明:
--   * luohuai codeX  modify: 条码使用16位业务规则，不再生成HUAXING前缀
--   * sku_spec 格式: 颜色 / 尺码（与 StockController 拼接格式一致）
--   * stock_record 冗余 product_name + sku_spec，type 用 INBOUND
--   * 幂等: 已存在的商品（按 id 匹配）会被忽略
-- ============================================================

-- luohuai codeX generate: demo dealer and managed specification codes are prerequisites for generated barcodes.
INSERT IGNORE INTO dealer (code,name,enabled) VALUES ('100','新旺角',1);
INSERT IGNORE INTO color_config (code,name,enabled,sort_order) VALUES
('01','白色',1,1),('02','黑色',1,2),('03','灰色',1,3),('04','蓝格',1,4),('05','红格',1,5),
('06','深蓝',1,6),('07','浅蓝',1,7),('08','卡其',1,8),('09','粉色',1,9),('10','米白',1,10),('11','蓝色',1,11);
INSERT IGNORE INTO size_config (code,name,enabled,sort_order) VALUES ('08','29',1,8),('09','30',1,9);

-- ---------- 商品主表 ----------
-- luohuai codeX  modify: each demo product has fixed dealer ownership and a numeric six-digit article number.
INSERT INTO product (id, category_id, dealer_id, product_code, name, cost_price, sell_price)
SELECT 1,5,id,'100001','纯棉圆领短袖T恤',29.00,49.00 FROM dealer WHERE code='100';
INSERT INTO product (id, category_id, dealer_id, product_code, name, cost_price, sell_price)
SELECT 2,6,id,'100002','格纹长袖衬衫',45.00,89.00 FROM dealer WHERE code='100';
INSERT INTO product (id, category_id, dealer_id, product_code, name, cost_price, sell_price)
SELECT 3,7,id,'100003','直筒牛仔裤',59.00,129.00 FROM dealer WHERE code='100';
INSERT INTO product (id, category_id, dealer_id, product_code, name, cost_price, sell_price)
SELECT 4,8,id,'100004','卡其色休闲裤',49.00,99.00 FROM dealer WHERE code='100';
INSERT INTO product (id, category_id, dealer_id, product_code, name, cost_price, sell_price)
SELECT 5,3,id,'100005','碎花连衣裙',69.00,159.00 FROM dealer WHERE code='100';
INSERT INTO product (id, category_id, dealer_id, product_code, name, cost_price, sell_price)
SELECT 6,4,id,'100006','经典牛仔外套',89.00,199.00 FROM dealer WHERE code='100';

-- ---------- SKU（颜色 / 尺码 组合）----------
-- product 1 纯棉圆领短袖T恤
INSERT INTO product_sku (id,product_id,color,color_config_id,size,size_config_id,barcode,stock_qty,version) VALUES
(101,1,'白色',(SELECT id FROM color_config WHERE code='01'),'M',(SELECT id FROM size_config WHERE code='02'),'1001000010201001',20,0),
(102,1,'白色',(SELECT id FROM color_config WHERE code='01'),'L',(SELECT id FROM size_config WHERE code='03'),'1001000010301001',18,0),
(103,1,'黑色',(SELECT id FROM color_config WHERE code='02'),'M',(SELECT id FROM size_config WHERE code='02'),'1001000010202001',25,0),
(104,1,'黑色',(SELECT id FROM color_config WHERE code='02'),'L',(SELECT id FROM size_config WHERE code='03'),'1001000010302001',22,0),
(105,1,'灰色',(SELECT id FROM color_config WHERE code='03'),'M',(SELECT id FROM size_config WHERE code='02'),'1001000010203001',15,0);

-- product 2 格纹长袖衬衫
INSERT INTO product_sku (id,product_id,color,color_config_id,size,size_config_id,barcode,stock_qty,version) VALUES
(106,2,'蓝格',(SELECT id FROM color_config WHERE code='04'),'M',(SELECT id FROM size_config WHERE code='02'),'1001000020204001',12,0),
(107,2,'蓝格',(SELECT id FROM color_config WHERE code='04'),'L',(SELECT id FROM size_config WHERE code='03'),'1001000020304001',10,0),
(108,2,'红格',(SELECT id FROM color_config WHERE code='05'),'M',(SELECT id FROM size_config WHERE code='02'),'1001000020205001',8,0);

-- product 3 直筒牛仔裤
INSERT INTO product_sku (id,product_id,color,color_config_id,size,size_config_id,barcode,stock_qty,version) VALUES
(109,3,'深蓝',(SELECT id FROM color_config WHERE code='06'),'29',(SELECT id FROM size_config WHERE code='08'),'1001000030806001',14,0),
(110,3,'深蓝',(SELECT id FROM color_config WHERE code='06'),'30',(SELECT id FROM size_config WHERE code='09'),'1001000030906001',16,0),
(111,3,'浅蓝',(SELECT id FROM color_config WHERE code='07'),'29',(SELECT id FROM size_config WHERE code='08'),'1001000030807001',9,0),
(112,3,'浅蓝',(SELECT id FROM color_config WHERE code='07'),'30',(SELECT id FROM size_config WHERE code='09'),'1001000030907001',11,0);

-- product 4 卡其色休闲裤
INSERT INTO product_sku (id,product_id,color,color_config_id,size,size_config_id,barcode,stock_qty,version) VALUES
(113,4,'卡其',(SELECT id FROM color_config WHERE code='08'),'L',(SELECT id FROM size_config WHERE code='03'),'1001000040308001',13,0),
(114,4,'卡其',(SELECT id FROM color_config WHERE code='08'),'XL',(SELECT id FROM size_config WHERE code='04'),'1001000040408001',7,0),
(115,4,'黑色',(SELECT id FROM color_config WHERE code='02'),'L',(SELECT id FROM size_config WHERE code='03'),'1001000040302001',10,0);

-- product 5 碎花连衣裙
INSERT INTO product_sku (id,product_id,color,color_config_id,size,size_config_id,barcode,stock_qty,version) VALUES
(116,5,'粉色',(SELECT id FROM color_config WHERE code='09'),'S',(SELECT id FROM size_config WHERE code='01'),'1001000050109001',6,0),
(117,5,'粉色',(SELECT id FROM color_config WHERE code='09'),'M',(SELECT id FROM size_config WHERE code='02'),'1001000050209001',8,0),
(118,5,'米白',(SELECT id FROM color_config WHERE code='10'),'M',(SELECT id FROM size_config WHERE code='02'),'1001000050210001',5,0);

-- product 6 经典牛仔外套
INSERT INTO product_sku (id,product_id,color,color_config_id,size,size_config_id,barcode,stock_qty,version) VALUES
(119,6,'蓝色',(SELECT id FROM color_config WHERE code='11'),'M',(SELECT id FROM size_config WHERE code='02'),'1001000060211001',9,0),
(120,6,'蓝色',(SELECT id FROM color_config WHERE code='11'),'L',(SELECT id FROM size_config WHERE code='03'),'1001000060311001',7,0);

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
