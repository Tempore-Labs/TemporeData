-- V32: align zy_user with the UserEntity JPA mapping.
-- The base zy_user (V1) created columns created_at/updated_at/real_name/account_expired_at,
-- but UserEntity maps create_time / density / must_change_password (plus username/password/
-- status/tenant_id/nickname/phone/email which already exist). A fresh provisioning therefore
-- fails runtime queries with "Unknown column 'userentity0_.create_time'". Add the missing
-- columns idempotently (MySQL has no "ADD COLUMN IF NOT EXISTS"), so this migration is safe on
-- both fresh schema and legacy DBs (existing columns are skipped).

-- create_time
SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_user' AND COLUMN_NAME = 'create_time') = 0,
                   'ALTER TABLE zy_user ADD COLUMN create_time DATETIME NULL', 'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;

-- density
SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_user' AND COLUMN_NAME = 'density') = 0,
                   'ALTER TABLE zy_user ADD COLUMN density VARCHAR(255) NULL', 'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;

-- must_change_password (Boolean -> TINYINT(1))
SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_user' AND COLUMN_NAME = 'must_change_password') = 0,
                   'ALTER TABLE zy_user ADD COLUMN must_change_password TINYINT(1) NULL', 'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;

-- zy_role: align with RoleEntity (V1 only created id/code/name).
SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_role' AND COLUMN_NAME = 'remark') = 0,
                   'ALTER TABLE zy_role ADD COLUMN remark VARCHAR(255) NULL', 'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;

SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_role' AND COLUMN_NAME = 'protected_role') = 0,
                   'ALTER TABLE zy_role ADD COLUMN protected_role TINYINT(1) NULL', 'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;

SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_role' AND COLUMN_NAME = 'data_scope') = 0,
                   'ALTER TABLE zy_role ADD COLUMN data_scope VARCHAR(255) NULL', 'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;

SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_role' AND COLUMN_NAME = 'create_time') = 0,
                   'ALTER TABLE zy_role ADD COLUMN create_time DATETIME NULL', 'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;