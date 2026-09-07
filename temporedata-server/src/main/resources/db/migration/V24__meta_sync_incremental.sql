-- Flyway migration V24 (meta P1): structure checksum for incremental sync + sync session log.
ALTER TABLE zy_meta_table ADD COLUMN checksum VARCHAR(64) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS zy_meta_sync_log (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    datasource_id VARCHAR(64),
    sync_type VARCHAR(16),                    -- FULL / INCREMENT
    status VARCHAR(16),                       -- RUNNING / SUCCESS / FAILED
    started_at VARCHAR(32),
    finished_at VARCHAR(32),
    tables_discovered INT,
    columns_discovered INT,
    unchanged_tables INT,
    errors VARCHAR(2000),
    KEY idx_msl_ds (datasource_id)
);