-- Flyway migration V25 (meta P3): table-structure change event log (lineage source for
-- quality invalidation / asset status / catalog refresh).
CREATE TABLE IF NOT EXISTS zy_meta_change (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    datasource_id VARCHAR(64),
    table_name VARCHAR(200),
    change_type VARCHAR(16),                  -- ADD / CHANGE / REMOVE
    checksum VARCHAR(64),
    synced_at VARCHAR(32),
    KEY idx_mc_ds (datasource_id, table_name, synced_at)
);