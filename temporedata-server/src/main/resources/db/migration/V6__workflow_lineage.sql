-- Flyway migration V6: P1-5 workflow task-level SQL lineage (per-node table dependencies).

CREATE TABLE IF NOT EXISTS zy_workflow_lineage (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    node_name VARCHAR(128),
    source_table VARCHAR(200),
    target_table VARCHAR(200),
    sql_type VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_wfl_workflow (workflow_id)
);