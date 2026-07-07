-- ============================================================
-- 华兴服装店 - 初始数据
-- ============================================================

-- 默认管理员: admin / admin123 (BCrypt)
INSERT IGNORE INTO sys_user (username, password, display_name, role) VALUES
('admin', '$2a$10$uSPtBju2fRXRbLBivT9Uk.iCFiB2pQ.P3L9QNP.sbAUUUt60mN79O', '老板', 'BOSS');

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
