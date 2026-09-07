-- Flyway migration V8: P2-8 absolute-time workflow baseline alarms.

CREATE TABLE IF NOT EXISTS alarm_baseline (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    workflow_id VARCHAR(36) NOT NULL,
    calendar_id VARCHAR(36),
    biz_date_mode VARCHAR(16) DEFAULT 'DAY',
    timezone VARCHAR(64) DEFAULT 'Asia/Shanghai',
    expect_time VARCHAR(8) NOT NULL,
    trigger_type VARCHAR(16) DEFAULT 'LATE',
    grace_seconds INT DEFAULT 300,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    notify_channels VARCHAR(255),
    operator VARCHAR(32),
    remark VARCHAR(255),
    create_time VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS alarm_record (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    baseline_id VARCHAR(36) NOT NULL,
    wf_instance_id VARCHAR(36),
    biz_date VARCHAR(10),
    alarm_type VARCHAR(16) NOT NULL,
    expect_finish_time VARCHAR(32),
    actual_finish_time VARCHAR(32),
    content VARCHAR(500),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    create_time VARCHAR(32),
    ack_by VARCHAR(32),
    ack_time VARCHAR(32),
    close_time VARCHAR(32),
    KEY idx_ar_baseline (baseline_id),
    KEY idx_ar_status (status)
);