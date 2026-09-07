-- Flyway migration V12: personal center profile fields.
-- Adds nickname / phone / email to zy_user for the personal center module.
--
-- Idempotent: V1__base_schema.sql already defines email + phone on zy_user, so a fresh
-- schema would fail on plain "ADD COLUMN phone/email" (Error 1060: Duplicate column).
-- Guard each column against existence via information_schema + PREPARE/EXECUTE so this
-- migration is safe on both fresh schema (V1 already has email/phone) and legacy DBs
-- (where none of the three exist yet). MySQL 8 has no "ADD COLUMN IF NOT EXISTS".

-- nickname
SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_user' AND COLUMN_NAME = 'nickname') = 0,
                   'ALTER TABLE zy_user ADD COLUMN nickname VARCHAR(50) NULL', 'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;

-- phone
SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_user' AND COLUMN_NAME = 'phone') = 0,
                   'ALTER TABLE zy_user ADD COLUMN phone VARCHAR(20) NULL', 'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;

-- email
SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_user' AND COLUMN_NAME = 'email') = 0,
                   'ALTER TABLE zy_user ADD COLUMN email VARCHAR(100) NULL', 'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;