-- Flyway migration V21 (P3): column-level lineage of workflow nodes (best-effort).
CREATE TABLE IF NOT EXISTS zy_column_lineage (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    node_name VARCHAR(128),
    target_table VARCHAR(200),
    target_column VARCHAR(128),
    source_table VARCHAR(200),
    source_column VARCHAR(128),
    dialect VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_zcl_workflow (workflow_id)
);