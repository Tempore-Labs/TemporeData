-- =============================================================================
-- V56: add_sys_preference_view_passwordless
--
-- Purpose:
--   Introduces idempotent CREATE TABLE IF NOT EXISTS for the remaining sys
--   modules that lacked front-end pages:
--     * sys/preference     -> zy_preference      (per-user key/value preferences)
--     * sys/view           -> zy_view            (logical view definitions)
--     * sys/passwordless   -> zy_passwordless    (passwordless login config)
--
-- Notes:
--   1. All idempotent CREATE TABLE IF NOT EXISTS.
--   2. id uses VARCHAR(36) to match the uuid2 primary keys used elsewhere.
-- =============================================================================

CREATE TABLE IF NOT EXISTS zy_preference (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME,
    update_by VARCHAR(32), tenant_id VARCHAR(32),
    pref_key VARCHAR(255), pref_value TEXT, user_id VARCHAR(32),
    create_time VARCHAR(32), update_time VARCHAR(32),
    PRIMARY KEY (id)
) COMMENT='Per-user preferences (sys.preference)';

CREATE TABLE IF NOT EXISTS zy_view (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME,
    update_by VARCHAR(32), tenant_id VARCHAR(32),
    name VARCHAR(255), status VARCHAR(32), description TEXT,
    PRIMARY KEY (id)
) COMMENT='Logical views (sys.view)';

CREATE TABLE IF NOT EXISTS zy_passwordless (
    id VARCHAR(36) NOT NULL,
    create_date_time DATETIME, create_by VARCHAR(32), update_date_time DATETIME,
    update_by VARCHAR(32), tenant_id VARCHAR(32),
    login_type VARCHAR(64), config TEXT, status VARCHAR(32),
    create_time VARCHAR(32), update_time VARCHAR(32),
    target VARCHAR(255), code VARCHAR(16),
    PRIMARY KEY (id)
) COMMENT='Passwordless login config (sys.passwordless)';