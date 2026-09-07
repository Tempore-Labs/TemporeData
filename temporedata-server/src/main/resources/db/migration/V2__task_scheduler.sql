-- Flyway migration V2: P0-1 Scheduler engine - task definitions, instances and logs

-- Task definition: a schedulable unit (cron / biz-date driven)
CREATE TABLE IF NOT EXISTS zy_task_define (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    task_type VARCHAR(32) NOT NULL DEFAULT 'WORKFLOW',
    target_ref VARCHAR(64),
    cron_expression VARCHAR(64) NOT NULL,
    biz_date_mode VARCHAR(16) NOT NULL DEFAULT 'NONE',
    calendar_id VARCHAR(36),
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    params_json TEXT,
    owner VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    version INT NOT NULL DEFAULT 0,
    create_by VARCHAR(32),
    update_by VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Task instance: one row per trigger/fire
CREATE TABLE IF NOT EXISTS zy_task_instance (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    instance_no BIGINT NOT NULL DEFAULT 0,
    trigger_time DATETIME,
    start_time DATETIME,
    finish_time DATETIME,
    biz_date VARCHAR(10),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    trigger_node VARCHAR(32),
    result_msg TEXT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_trigger (task_id, trigger_time),
    KEY idx_task_instance_status (task_id, status)
);

-- Task execution log: per-instance debug/audit trail
CREATE TABLE IF NOT EXISTS zy_task_log (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    instance_id VARCHAR(36) NOT NULL,
    level VARCHAR(8) NOT NULL DEFAULT 'INFO',
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task_log_instance (instance_id)
);