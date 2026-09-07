-- =============================================================================
-- V31: 数据血缘模块重新设计（设计文档 §3）——血链表迁移/重命名 + 富语义字段增强
-- 说明：
--   1. 把旧 `zy_*` 血链表（zy_lineage / zy_workflow_lineage / zy_column_lineage /
--      zy_meta_table）迁移/重命名为 `temporedata_*`，保留存量数据；若无旧表则
--      CREATE TABLE 新表（兼容全新环境）。
--   2. 对 `temporedata_lineage` 增强富语义/审计字段：source_srs（来源枚举）、
--      lineage_details_json（LONGTEXT，存 LineageDetails 全量）、created_by /
--      updated_by / created_at / updated_at（时间窗/审计，旧行回填便于向前兼容）。
--   3. 脚本尽量幂等、可重复执行（用存储过程 + information_schema 判定存在性）。
--
--   注意：来源枚举列使用 source_srs 而非 source，因为旧 zy_lineage 已存在遗留
--         source/target 占位列（与实体遗留字段对齐），为避免重复列名冲突。
-- =============================================================================

DELIMITER $$

-- ---------------------------------------------------------------
-- 1) 重命名/迁移血链表（或新建）+ 确保 4 张 temporedata_* 表存在
-- ---------------------------------------------------------------
DROP PROCEDURE IF EXISTS td_lineage_v31_rename_tables$$
CREATE PROCEDURE td_lineage_v31_rename_tables()
BEGIN
    DECLARE old_exists INT DEFAULT 0;
    DECLARE new_exists INT DEFAULT 0;

    -- zy_lineage -> temporedata_lineage
    SET old_exists = 0; SET new_exists = 0;
    SELECT COUNT(*) INTO old_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_lineage';
    SELECT COUNT(*) INTO new_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temporedata_lineage';
    IF old_exists = 1 AND new_exists = 0 THEN
        RENAME TABLE zy_lineage TO temporedata_lineage;
    ELSEIF old_exists = 1 AND new_exists = 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'td_lineage_v31: both zy_lineage and temporedata_lineage exist, manual reconciliation required';
    ELSE
        CREATE TABLE IF NOT EXISTS temporedata_lineage (
            id VARCHAR(32) NOT NULL PRIMARY KEY,
            create_date_time DATETIME(6),
            create_by VARCHAR(32),
            update_date_time DATETIME(6),
            update_by VARCHAR(32),
            tenant_id VARCHAR(32),
            has_cycle TINYINT(1) DEFAULT 0,
            summary TEXT,
            name VARCHAR(255),
            node_type VARCHAR(255),
            in_degree INT DEFAULT 0,
            out_degree INT DEFAULT 0,
            total INT DEFAULT 0,
            source_id VARCHAR(255),
            target_id VARCHAR(255),
            source_name VARCHAR(255),
            target_name VARCHAR(255),
            found TINYINT(1) DEFAULT 0,
            source VARCHAR(255),
            target VARCHAR(255),
            edge_type VARCHAR(255),
            task_name VARCHAR(255)
        );
    END IF;

    -- zy_meta_table -> temporedata_meta_table
    SET old_exists = 0; SET new_exists = 0;
    SELECT COUNT(*) INTO old_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_meta_table';
    SELECT COUNT(*) INTO new_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temporedata_meta_table';
    IF old_exists = 1 AND new_exists = 0 THEN
        RENAME TABLE zy_meta_table TO temporedata_meta_table;
    ELSEIF old_exists = 1 AND new_exists = 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'td_lineage_v31: both zy_meta_table and temporedata_meta_table exist, manual reconciliation required';
    ELSE
        CREATE TABLE IF NOT EXISTS temporedata_meta_table (
            id VARCHAR(36) NOT NULL PRIMARY KEY,
            datasource_id VARCHAR(64),
            schema_name VARCHAR(128),
            table_name VARCHAR(200),
            table_comment VARCHAR(500),
            row_count BIGINT,
            data_size BIGINT,
            data_level_code VARCHAR(32),
            tags VARCHAR(1000),
            status VARCHAR(16) DEFAULT 'ACTIVE',
            last_sync_time VARCHAR(32),
            checksum VARCHAR(64),
            lineage_count INT DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    END IF;

    -- zy_workflow_lineage -> temporedata_workflow_lineage
    SET old_exists = 0; SET new_exists = 0;
    SELECT COUNT(*) INTO old_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_workflow_lineage';
    SELECT COUNT(*) INTO new_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temporedata_workflow_lineage';
    IF old_exists = 1 AND new_exists = 0 THEN
        RENAME TABLE zy_workflow_lineage TO temporedata_workflow_lineage;
    ELSEIF old_exists = 1 AND new_exists = 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'td_lineage_v31: both zy_workflow_lineage and temporedata_workflow_lineage exist, manual reconciliation required';
    ELSE
        CREATE TABLE IF NOT EXISTS temporedata_workflow_lineage (
            id VARCHAR(36) NOT NULL PRIMARY KEY,
            workflow_id VARCHAR(36) NOT NULL,
            node_id VARCHAR(64) NOT NULL,
            node_name VARCHAR(128),
            source_table VARCHAR(200),
            target_table VARCHAR(200),
            sql_type VARCHAR(32),
            dialect VARCHAR(32),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    END IF;

    -- zy_column_lineage -> temporedata_column_lineage
    SET old_exists = 0; SET new_exists = 0;
    SELECT COUNT(*) INTO old_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_column_lineage';
    SELECT COUNT(*) INTO new_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temporedata_column_lineage';
    IF old_exists = 1 AND new_exists = 0 THEN
        RENAME TABLE zy_column_lineage TO temporedata_column_lineage;
    ELSEIF old_exists = 1 AND new_exists = 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'td_lineage_v31: both zy_column_lineage and temporedata_column_lineage exist, manual reconciliation required';
    ELSE
        CREATE TABLE IF NOT EXISTS temporedata_column_lineage (
            id VARCHAR(36) NOT NULL PRIMARY KEY,
            workflow_id VARCHAR(36) NOT NULL,
            node_id VARCHAR(64) NOT NULL,
            node_name VARCHAR(128),
            target_table VARCHAR(200),
            target_column VARCHAR(128),
            source_table VARCHAR(200),
            source_column VARCHAR(128),
            dialect VARCHAR(32),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    END IF;
END$$

-- ---------------------------------------------------------------
-- 2) 增强 temporedata_lineage：富语义/审计字段（存在性判定幂等）
-- ---------------------------------------------------------------
DROP PROCEDURE IF EXISTS td_lineage_v31_enhance_lineage$$
CREATE PROCEDURE td_lineage_v31_enhance_lineage()
BEGIN
    DECLARE col_exists INT DEFAULT 0;

    SET col_exists = 0;
    SELECT COUNT(*) INTO col_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temporedata_lineage' AND COLUMN_NAME = 'source_srs';
    IF col_exists = 0 THEN ALTER TABLE temporedata_lineage ADD COLUMN source_srs VARCHAR(32); END IF;

    SET col_exists = 0;
    SELECT COUNT(*) INTO col_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temporedata_lineage' AND COLUMN_NAME = 'lineage_details_json';
    IF col_exists = 0 THEN ALTER TABLE temporedata_lineage ADD COLUMN lineage_details_json LONGTEXT; END IF;

    SET col_exists = 0;
    SELECT COUNT(*) INTO col_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temporedata_lineage' AND COLUMN_NAME = 'created_by';
    IF col_exists = 0 THEN ALTER TABLE temporedata_lineage ADD COLUMN created_by VARCHAR(64); END IF;

    SET col_exists = 0;
    SELECT COUNT(*) INTO col_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temporedata_lineage' AND COLUMN_NAME = 'updated_by';
    IF col_exists = 0 THEN ALTER TABLE temporedata_lineage ADD COLUMN updated_by VARCHAR(64); END IF;

    SET col_exists = 0;
    SELECT COUNT(*) INTO col_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temporedata_lineage' AND COLUMN_NAME = 'created_at';
    IF col_exists = 0 THEN ALTER TABLE temporedata_lineage ADD COLUMN created_at DATETIME; END IF;

    SET col_exists = 0;
    SELECT COUNT(*) INTO col_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temporedata_lineage' AND COLUMN_NAME = 'updated_at';
    IF col_exists = 0 THEN ALTER TABLE temporedata_lineage ADD COLUMN updated_at DATETIME; END IF;

    -- 旧行回填：为存量数据补齐审计时间戳（向前兼容）
    UPDATE temporedata_lineage SET created_at = update_date_time WHERE created_at IS NULL AND update_date_time IS NOT NULL;
    UPDATE temporedata_lineage SET updated_at = update_date_time WHERE updated_at IS NULL AND update_date_time IS NOT NULL;
END$$

-- ---------------------------------------------------------------
-- 3) 依次执行并清理临时存储过程
-- ---------------------------------------------------------------
CALL td_lineage_v31_rename_tables();$$
CALL td_lineage_v31_enhance_lineage();$$ 

DROP PROCEDURE IF EXISTS td_lineage_v31_rename_tables;$$
DROP PROCEDURE IF EXISTS td_lineage_v31_enhance_lineage;$$

DELIMITER ;