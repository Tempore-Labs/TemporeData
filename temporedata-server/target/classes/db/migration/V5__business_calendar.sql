-- Flyway migration V5: P1-4 business calendar (workdays) and day-cut (切日) management.

-- Custom calendars: define a business domain's working days.
CREATE TABLE IF NOT EXISTS cal_calendar (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    workday_mode VARCHAR(16) NOT NULL DEFAULT 'CUSTOM',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    week_workdays VARCHAR(32) DEFAULT '1,2,3,4,5',
    create_by VARCHAR(32),
    update_by VARCHAR(32),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Individual day overrides (used when workday_mode = CUSTOM).
CREATE TABLE IF NOT EXISTS cal_day_meta (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    calendar_id VARCHAR(36) NOT NULL,
    biz_date VARCHAR(10) NOT NULL,
    is_workday TINYINT(1) NOT NULL DEFAULT 1,
    day_type VARCHAR(16) NOT NULL DEFAULT 'WORKDAY',
    remark VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cal_day (calendar_id, biz_date)
);

-- Day-cut time per calendar (natural-day boundary dividing biz_date).
CREATE TABLE IF NOT EXISTS cal_cut_time (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    calendar_id VARCHAR(36) NOT NULL,
    cut_hour INT NOT NULL DEFAULT 0,
    cut_minute INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cal_cut (calendar_id)
);

-- Backfill queue: missed business dates to be re-fired for a task.
CREATE TABLE IF NOT EXISTS biz_date_queue (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL,
    biz_date VARCHAR(10) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    create_time DATETIME,
    UNIQUE KEY uk_queue_task_biz (task_id, biz_date)
);