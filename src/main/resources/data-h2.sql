-- 默认管理员: admin / admin123 (BCrypt)
INSERT INTO sys_user (id, username, password, display_name, role, enabled, create_time) VALUES
(1, 'admin', '$2a$10$uSPtBju2fRXRbLBivT9Uk.iCFiB2pQ.P3L9QNP.sbAUUUt60mN79O', '老板', 'BOSS', TRUE, NOW());

-- 默认店员: user / user123 (BCrypt)
INSERT INTO sys_user (id, username, password, display_name, role, enabled, create_time) VALUES
(2, 'user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '店员', 'CLERK', TRUE, NOW());

-- 默认商品分类
INSERT INTO category (id, name, parent_id, sort_order) VALUES
(1, '上衣', NULL, 1),
(2, '裤装', NULL, 2),
(3, '裙装', NULL, 3),
(4, '外套', NULL, 4),
(5, 'T恤', 1, 1),
(6, '衬衫', 1, 2),
(7, '牛仔裤', 2, 1),
(8, '休闲裤', 2, 2);
