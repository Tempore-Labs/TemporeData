-- P2: workflow versioning (snapshot history for preview / rollback).
CREATE TABLE IF NOT EXISTS zy_wf_version (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME, update_by VARCHAR(32), tenant_id VARCHAR(32),
    workflow_id VARCHAR(36) NOT NULL,
    version_no INT NOT NULL,
    name VARCHAR(255),
    nodes_json LONGTEXT NULL,
    edges_json LONGTEXT NULL,
    remark VARCHAR(255),
    create_time VARCHAR(32),
    PRIMARY KEY (id),
    KEY idx_wf_version_workflow (workflow_id)
) COMMENT='Workflow definition version history';