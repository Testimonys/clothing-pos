-- luohuai codeX generate: additive MySQL 8 migration for dealer ownership, managed specifications and 16-digit barcodes.
-- Back up the selected database before running this file.
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS dealer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code CHAR(3) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    contact_name VARCHAR(50) NULL,
    phone VARCHAR(50) NULL,
    address VARCHAR(255) NULL,
    remark VARCHAR(500) NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO dealer(code, name, enabled) VALUES ('100', '新旺角', 1);

SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product' AND COLUMN_NAME='dealer_id'),
    'SELECT 1', 'ALTER TABLE product ADD COLUMN dealer_id BIGINT NULL AFTER product_code');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;

-- luohuai codeX generate: current data is confirmed test data from 新旺角; future products choose a dealer explicitly.
UPDATE product SET dealer_id=(SELECT id FROM dealer WHERE code='100' LIMIT 1) WHERE dealer_id IS NULL;

SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product' AND INDEX_NAME='uk_product_code'),
    'ALTER TABLE product DROP INDEX uk_product_code', 'SELECT 1');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;

SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product' AND INDEX_NAME='uk_product_dealer_code'),
    'SELECT 1', 'ALTER TABLE product ADD UNIQUE KEY uk_product_dealer_code(dealer_id, product_code)');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;

CREATE TABLE IF NOT EXISTS color_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code CHAR(2) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL UNIQUE,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO color_config(code,name,enabled,sort_order) VALUES ('00','无颜色',1,0);

-- luohuai codeX generate: promote distinct legacy test colors into the managed dictionary without treating 0无 as real data.
SET @color_base=(SELECT COALESCE(MAX(CAST(code AS UNSIGNED)),0) FROM color_config WHERE code<>'00');
INSERT IGNORE INTO color_config(code,name,enabled,sort_order)
SELECT LPAD(@color_base + ROW_NUMBER() OVER(ORDER BY legacy.name),2,'0'), legacy.name,1,
       @color_base + ROW_NUMBER() OVER(ORDER BY legacy.name)
FROM (SELECT DISTINCT TRIM(color) AS name FROM product_sku WHERE TRIM(color)<>'' AND TRIM(color)<>'0无') legacy
LEFT JOIN color_config existing ON existing.name=legacy.name
WHERE existing.id IS NULL;

SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='size_config' AND COLUMN_NAME='code'),
    'SELECT 1', 'ALTER TABLE size_config ADD COLUMN code CHAR(2) NULL AFTER id');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;
SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='size_config' AND COLUMN_NAME='enabled'),
    'SELECT 1', 'ALTER TABLE size_config ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 AFTER name');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;

-- luohuai codeX generate: preserve agreed codes for existing standard sizes before allocating custom values.
UPDATE size_config SET code=CASE name WHEN '均码' THEN '00' WHEN 'S' THEN '01' WHEN 'M' THEN '02' WHEN 'L' THEN '03'
    WHEN 'XL' THEN '04' WHEN '2XL' THEN '05' WHEN '3XL' THEN '06' WHEN '4XL' THEN '07' ELSE code END WHERE code IS NULL;
SET @size_code=7;
UPDATE size_config SET code=LPAD((@size_code:=@size_code+1),2,'0') WHERE code IS NULL ORDER BY id;
INSERT IGNORE INTO size_config(code,name,enabled,sort_order) VALUES ('00','均码',1,0);

-- luohuai codeX generate: retain real legacy size labels while leaving the 0均码 placeholder unresolved.
SET @size_base=(SELECT COALESCE(MAX(CAST(code AS UNSIGNED)),0) FROM size_config WHERE code<>'00');
INSERT IGNORE INTO size_config(code,name,enabled,sort_order)
SELECT LPAD(@size_base + ROW_NUMBER() OVER(ORDER BY legacy.name),2,'0'), legacy.name,1,
       @size_base + ROW_NUMBER() OVER(ORDER BY legacy.name)
FROM (SELECT DISTINCT TRIM(size) AS name FROM product_sku WHERE TRIM(size)<>'' AND TRIM(size)<>'0均码') legacy
LEFT JOIN size_config existing ON existing.name=legacy.name
WHERE existing.id IS NULL;

SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='size_config' AND INDEX_NAME='uk_size_code'),
    'SELECT 1', 'ALTER TABLE size_config MODIFY code CHAR(2) NOT NULL, ADD UNIQUE KEY uk_size_code(code)');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;

SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product_sku' AND COLUMN_NAME='color_config_id'),
    'SELECT 1', 'ALTER TABLE product_sku ADD COLUMN color_config_id BIGINT NULL AFTER color');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;
SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product_sku' AND COLUMN_NAME='size_config_id'),
    'SELECT 1', 'ALTER TABLE product_sku ADD COLUMN size_config_id BIGINT NULL AFTER size');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;

-- luohuai codeX generate: exact-name legacy matches gain stable IDs; unknown placeholders remain pending.
UPDATE product_sku ps JOIN color_config c ON c.name=ps.color SET ps.color_config_id=c.id WHERE ps.color_config_id IS NULL;
UPDATE product_sku ps JOIN size_config s ON s.name=ps.size SET ps.size_config_id=s.id WHERE ps.size_config_id IS NULL;

-- luohuai codeX generate: add referential constraints only after compatible legacy rows have been mapped.
SET @catalog_ddl=IF(EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='product' AND CONSTRAINT_NAME='fk_product_dealer'),
    'SELECT 1','ALTER TABLE product ADD CONSTRAINT fk_product_dealer FOREIGN KEY(dealer_id) REFERENCES dealer(id)');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;
SET @catalog_ddl=IF(EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='product_sku' AND CONSTRAINT_NAME='fk_sku_color_config'),
    'SELECT 1','ALTER TABLE product_sku ADD CONSTRAINT fk_sku_color_config FOREIGN KEY(color_config_id) REFERENCES color_config(id)');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;
SET @catalog_ddl=IF(EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='product_sku' AND CONSTRAINT_NAME='fk_sku_size_config'),
    'SELECT 1','ALTER TABLE product_sku ADD CONSTRAINT fk_sku_size_config FOREIGN KEY(size_config_id) REFERENCES size_config(id)');
PREPARE catalog_migration FROM @catalog_ddl; EXECUTE catalog_migration; DEALLOCATE PREPARE catalog_migration;

CREATE TABLE IF NOT EXISTS barcode_sequence(prefix CHAR(13) PRIMARY KEY, current_value INT NOT NULL DEFAULT 0)
    ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- luohuai codeX generate: replace eligible HUAXING test codes; incomplete rows remain visibly pending instead of receiving fabricated segments.
UPDATE product_sku ps
JOIN product p ON p.id=ps.product_id
JOIN dealer d ON d.id=p.dealer_id
JOIN size_config s ON s.id=ps.size_config_id
JOIN color_config c ON c.id=ps.color_config_id
SET ps.barcode=CONCAT(d.code,LPAD(p.product_code,6,'0'),s.code,c.code,'001')
WHERE ps.barcode LIKE 'HUAXING%' AND p.product_code REGEXP '^[0-9]{1,6}$';

INSERT INTO barcode_sequence(prefix,current_value)
SELECT LEFT(barcode,13),MAX(CAST(RIGHT(barcode,3) AS UNSIGNED)) FROM product_sku
WHERE barcode REGEXP '^[0-9]{16}$' GROUP BY LEFT(barcode,13)
ON DUPLICATE KEY UPDATE current_value=GREATEST(barcode_sequence.current_value,VALUES(current_value));
CREATE TABLE IF NOT EXISTS product_dealer_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    old_dealer_id BIGINT NULL,
    new_dealer_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(product_id) REFERENCES product(id),
    FOREIGN KEY(old_dealer_id) REFERENCES dealer(id),
    FOREIGN KEY(new_dealer_id) REFERENCES dealer(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
