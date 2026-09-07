-- Flyway migration V3: P0-2 workflow DAG runtime - instances, node instances and logs.

-- One row per workflow run (instance).
CREATE TABLE IF NOT EXISTS zy_wf_instance (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL,
    task_instance_id VARCHAR(36),
    biz_date VARCHAR(10),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    trigger_type VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
    start_time DATETIME,
    finish_time DATETIME,
    result_msg TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_wf_instance_wf (workflow_id)
);

-- One row per node within a workflow run.
CREATE TABLE IF NOT EXISTS zy_wf_node_instance (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    instance_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    node_name VARCHAR(128),
    node_type VARCHAR(32),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    start_time DATETIME,
    finish_time DATETIME,
    retry_times INT NOT NULL DEFAULT 0,
    result TEXT,
    error_msg TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_wfn_instance (instance_id)
);

-- Runtime log trail for a workflow instance / node.
CREATE TABLE IF NOT EXISTS zy_wf_instance_log (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    instance_id VARCHAR(36) NOT NULL,
    node_instance_id VARCHAR(36),
    level VARCHAR(8) NOT NULL DEFAULT 'INFO',
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_wfil_instance (instance_id)
);