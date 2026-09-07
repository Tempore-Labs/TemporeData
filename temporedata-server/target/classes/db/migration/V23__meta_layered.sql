-- Flyway migration V23 (meta P0): layered metadata tables (design §3).
CREATE TABLE IF NOT EXISTS zy_meta_table (
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_mt_ds (datasource_id, table_name)
);

CREATE TABLE IF NOT EXISTS zy_meta_column (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    table_id VARCHAR(36),
    datasource_id VARCHAR(64),
    table_name VARCHAR(200),
    column_name VARCHAR(200),
    column_type VARCHAR(64),
    column_size INT,
    nullable TINYINT(1),
    default_value VARCHAR(500),
    comment VARCHAR(500),
    primary_key TINYINT(1) DEFAULT 0,
    ordinal_position INT,
    sensitive_flag TINYINT(1) DEFAULT 0,
    data_level_code VARCHAR(32),
    status VARCHAR(16) DEFAULT 'ACTIVE',
    KEY idx_mc_table (table_id),
    KEY idx_mc_ds (datasource_id, table_name)
);