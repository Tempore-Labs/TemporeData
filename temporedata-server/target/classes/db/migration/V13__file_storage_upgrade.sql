-- Flyway migration V13: rebuild zy_file to match FileEntity + storage upgrades.
-- The table previously drifted from the entity (legacy file_path / NOT NULL
-- columns). Recreate it aligned with FileEntity so the generic FileStorage
-- module S1 (biz attribution, dedup, security, soft delete) works correctly.

DROP TABLE IF EXISTS zy_file;

CREATE TABLE zy_file (
    id                 VARCHAR(36)  NOT NULL PRIMARY KEY,
    create_date_time   DATETIME(6),
    create_by          VARCHAR(32),
    update_date_time   DATETIME(6),
    update_by          VARCHAR(32),
    tenant_id          VARCHAR(32),
    name               VARCHAR(255),
    original_name      VARCHAR(255),
    file_size          BIGINT,
    file_type          VARCHAR(64),
    create_time        VARCHAR(40),
    biz_type           VARCHAR(64),
    biz_id             VARCHAR(64),
    ext                VARCHAR(32),
    md5                VARCHAR(64),
    storage_key        VARCHAR(255),
    storage_type       VARCHAR(32),
    status             VARCHAR(16)  DEFAULT 'READY',
    scan_status        VARCHAR(16)  DEFAULT 'NONE',
    del_flag           TINYINT(1)   DEFAULT 0
) DEFAULT CHARSET = utf8mb4;