-- Flyway migration V4: P0-3 runtime management - instance control fields + run command log.

-- Control fields on workflow instance (priority / pool / pause / stop markers).
ALTER TABLE zy_wf_instance
    ADD COLUMN priority INT NOT NULL DEFAULT 5,
    ADD COLUMN pool VARCHAR(32) NOT NULL DEFAULT 'default',
    ADD COLUMN paused_at DATETIME NULL,
    ADD COLUMN stopped_at DATETIME NULL;

-- Auditable runtime command log (pause/resume/stop/rerun).
CREATE TABLE IF NOT EXISTS zy_wf_run_command (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    instance_id VARCHAR(36) NOT NULL,
    type VARCHAR(16) NOT NULL,
    scope VARCHAR(16) NOT NULL DEFAULT 'ALL',
    target_node_id VARCHAR(64),
    state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    operator VARCHAR(64),
    reason VARCHAR(255),
    create_time DATETIME,
    done_time DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_wfrc_instance (instance_id)
);