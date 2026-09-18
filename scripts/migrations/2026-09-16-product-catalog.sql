-- luohuai codeX generate: additive, repeatable migration for existing MySQL databases; preserves products, SKUs and stock.
-- Run against the selected clothing_pos database BEFORE deploying the new backend.
SET NAMES utf8mb4;

SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'product_code'),
    'SELECT 1', 'ALTER TABLE product ADD COLUMN product_code VARCHAR(50) NULL AFTER name');
PREPARE catalog_migration FROM @catalog_ddl;
EXECUTE catalog_migration;
DEALLOCATE PREPARE catalog_migration;

SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'unit'),
    'SELECT 1', 'ALTER TABLE product ADD COLUMN unit VARCHAR(20) NOT NULL DEFAULT ''件'' AFTER product_code');
PREPARE catalog_migration FROM @catalog_ddl;
EXECUTE catalog_migration;
DEALLOCATE PREPARE catalog_migration;

SET @catalog_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'uk_product_code'),
    'SELECT 1', 'ALTER TABLE product ADD UNIQUE KEY uk_product_code (product_code)');
PREPARE catalog_migration FROM @catalog_ddl;
EXECUTE catalog_migration;
DEALLOCATE PREPARE catalog_migration;
