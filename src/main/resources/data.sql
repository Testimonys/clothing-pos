-- ============================================================
-- 华兴服装店 - 初始数据
-- ============================================================

-- 确保任意客户端（含 MySQL docker-entrypoint 默认 latin1 连接）执行时
-- 中文字符集正确，避免双重编码乱码
SET NAMES utf8mb4;

-- 默认管理员: admin / admin123 (BCrypt)
INSERT IGNORE INTO sys_user (username, password, display_name, role) VALUES
('admin', '$2a$10$uSPtBju2fRXRbLBivT9Uk.iCFiB2pQ.P3L9QNP.sbAUUUt60mN79O', '老板', 'BOSS');

-- 默认店员: user / user123 (BCrypt)
INSERT IGNORE INTO sys_user (username, password, display_name, role) VALUES
('user', '$2a$10$1WCJy5wTZ5cCgMv2am9LHetv4tpSpyaG7FXmGxTFDdXaQ8KrUd20W', '店员', 'CLERK');

-- 默认商品分类
INSERT IGNORE INTO category (id, name, parent_id, sort_order) VALUES
(1, '上衣', NULL, 1),
(2, '裤装', NULL, 2),
(3, '裙装', NULL, 3),
(4, '外套', NULL, 4),
(5, 'T恤', 1, 1),
(6, '衬衫', 1, 2),
(7, '牛仔裤', 2, 1),
(8, '休闲裤', 2, 2);

-- luohuai codeX  modify: stable codes are never reused; 00 has the confirmed no-dimension meaning.
INSERT IGNORE INTO size_config (code, name, enabled, sort_order) VALUES
('00', '均码', 1, 0), ('01', 'S', 1, 1), ('02', 'M', 1, 2), ('03', 'L', 1, 3),
('04', 'XL', 1, 4), ('05', '2XL', 1, 5), ('06', '3XL', 1, 6), ('07', '4XL', 1, 7);

-- luohuai codeX generate: 00 is selectable only when the product truly has no color distinction.
INSERT IGNORE INTO color_config (code, name, enabled, sort_order) VALUES ('00', '无颜色', 1, 0);

-- luohuai codeX generate: confirmed source dealer for the legacy test111.xls catalog.
INSERT IGNORE INTO dealer (code, name, enabled) VALUES ('100', '新旺角', 1);
