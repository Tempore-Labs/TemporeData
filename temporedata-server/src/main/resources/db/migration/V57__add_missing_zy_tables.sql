-- =============================================================================
-- V57: add_missing_zy_tables
--
-- Purpose:
--   Idempotent CREATE TABLE IF NOT EXISTS for 10 tables referenced by @Entity
--   classes but missing from the schema (would 500 at runtime):
--     * zy_catalog          gov/catalog
--     * zy_service          svc/service
--     * zy_form_submission  svc/form (submissions)
--     * zy_security         gov/security
--     * zy_notify           sys/notify
--     * zy_settings         sys/settings
--     * zy_message          sys/message
--     * zy_auth             sys/auth
--     * zy_dashboard        ops/dashboard
--     * zy_schedule         dev/schedule
--
-- Notes:
--   1. All idempotent CREATE TABLE IF NOT EXISTS.
--   2. id uses VARCHAR(36) to match uuid2 primary keys.
--   3. Reserved identifiers (sql/comment/value/user) are backtick-quoted.
-- =============================================================================

CREATE TABLE IF NOT EXISTS zy_catalog (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME, update_by VARCHAR(32), tenant_id VARCHAR(32),
    label VARCHAR(255), type VARCHAR(64), datasource_name VARCHAR(255), schema_name VARCHAR(255), table_name VARCHAR(255),
    row_count BIGINT, `comment` TEXT, is_leaf BOOLEAN, column_name VARCHAR(255), data_type VARCHAR(64), column_size INTEGER,
    decimal_digits INTEGER, default_value TEXT, ordinal_position INTEGER, is_nullable BOOLEAN, is_primary_key BOOLEAN,
    data_level_id VARCHAR(64), table_id VARCHAR(64), column_id VARCHAR(64), data_category_id VARCHAR(64), relation_type VARCHAR(64),
    relation_name VARCHAR(255), datasource_id VARCHAR(64), sync_time VARCHAR(64), PRIMARY KEY (id)
) COMMENT='Data catalog nodes (gov.catalog)';

CREATE TABLE IF NOT EXISTS zy_service (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME, update_by VARCHAR(32), tenant_id VARCHAR(32),
    name VARCHAR(255), description TEXT, datasource_id VARCHAR(64), `sql` TEXT, method VARCHAR(16), path VARCHAR(255),
    cache_ttl VARCHAR(64), api_key VARCHAR(255), status VARCHAR(32), call_count BIGINT, last_call_time VARCHAR(64),
    create_time VARCHAR(32), update_time VARCHAR(32), PRIMARY KEY (id)
) COMMENT='Data API service definitions (svc.service)';

CREATE TABLE IF NOT EXISTS zy_form_submission (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, form_id VARCHAR(36), data TEXT, submitter_ip VARCHAR(64), submit_time VARCHAR(32),
    PRIMARY KEY (id)
) COMMENT='Dynamic-form submissions (svc.form)';

CREATE TABLE IF NOT EXISTS zy_security (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME, update_by VARCHAR(32), tenant_id VARCHAR(32),
    name VARCHAR(255), code VARCHAR(64), parent_id VARCHAR(64), level_id VARCHAR(64), description TEXT, level_name VARCHAR(64),
    level_code VARCHAR(32), create_time VARCHAR(32), sort_order INTEGER, rule_type VARCHAR(32), mask_pattern TEXT,
    datasource_id VARCHAR(64), table_name VARCHAR(255), column_name VARCHAR(255), status INTEGER, update_time VARCHAR(32),
    PRIMARY KEY (id)
) COMMENT='Security governance classifications (gov.security)';

CREATE TABLE IF NOT EXISTS zy_notify (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME, update_by VARCHAR(32), tenant_id VARCHAR(32),
    name VARCHAR(255), status VARCHAR(32), description TEXT, PRIMARY KEY (id)
) COMMENT='Notifications (sys.notify)';

CREATE TABLE IF NOT EXISTS zy_settings (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME, update_by VARCHAR(32), tenant_id VARCHAR(32),
    name VARCHAR(255), status VARCHAR(32), description TEXT, PRIMARY KEY (id)
) COMMENT='System settings (sys.settings)';

CREATE TABLE IF NOT EXISTS zy_message (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME, update_by VARCHAR(32), tenant_id VARCHAR(32),
    name VARCHAR(255), status VARCHAR(32), description TEXT, PRIMARY KEY (id)
) COMMENT='Messages (sys.message)';

CREATE TABLE IF NOT EXISTS zy_auth (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME, update_by VARCHAR(32), tenant_id VARCHAR(32),
    username VARCHAR(128), password VARCHAR(255), token TEXT, `user` TEXT, user_id VARCHAR(64), tenant_name VARCHAR(128),
    PRIMARY KEY (id)
) COMMENT='Auth records (sys.auth)';

CREATE TABLE IF NOT EXISTS zy_dashboard (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME, update_by VARCHAR(32), tenant_id VARCHAR(32),
    datasource_count INTEGER, table_count INTEGER, total_rows BIGINT, sync_task_count INTEGER, sync_success_count INTEGER,
    sync_fail_count INTEGER, quality_rule_count INTEGER, quality_pass_count INTEGER, quality_fail_count INTEGER, workflow_count INTEGER,
    api_count INTEGER, api_call_total BIGINT, name VARCHAR(255), `value` INTEGER, table_name VARCHAR(255), row_count BIGINT,
    task_name VARCHAR(255), status VARCHAR(32), last_run_time VARCHAR(64), hour VARCHAR(32), success INTEGER, failed INTEGER,
    target_table VARCHAR(255), error_msg TEXT, rule_name VARCHAR(255), column_name VARCHAR(255), rule_type VARCHAR(64),
    description TEXT, type VARCHAR(64), PRIMARY KEY (id)
) COMMENT='Dashboard aggregation (ops.dashboard)';

CREATE TABLE IF NOT EXISTS zy_schedule (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME, update_by VARCHAR(32), tenant_id VARCHAR(32),
    cron_expression VARCHAR(255), enabled INTEGER, PRIMARY KEY (id)
) COMMENT='Schedule definitions (dev.schedule)';