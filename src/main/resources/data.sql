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

-- 默认尺码标签（可在系统设置→标签管理中增删改）
INSERT IGNORE INTO size_config (name, sort_order) VALUES
('S', 1), ('M', 2), ('L', 3), ('XL', 4), ('2XL', 5), ('3XL', 6), ('4XL', 7);
