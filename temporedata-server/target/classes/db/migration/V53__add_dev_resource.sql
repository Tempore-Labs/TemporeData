-- =============================================================================
-- V53: add_dev_resource - Data-dev resource assets (scripts/config/files)
--
-- Purpose:
--   [DEV-RESOURCE] Introduces the resource-center table used by the
--   temporedata-service `dev.resource` package to manage data-development
--   resources (SQL/Python scripts, config snippets, reference files).
--
-- Notes:
--   1. Pure NEW table -> idempotent CREATE TABLE IF NOT EXISTS.
--   2. Mirrors ResourceEntity: id = 32-char UUID; description stored as TEXT.
--   3. status field semantics: AVAILABLE / IN_USE / ARCHIVED / DISABLED.
-- =============================================================================

CREATE TABLE IF NOT EXISTS zy_resource (
    id VARCHAR(32) NOT NULL COMMENT 'Primary key: 32-char UUID',
    create_date_time DATETIME COMMENT 'Audit timestamp: created',
    create_by VARCHAR(32) COMMENT 'Creator user id',
    update_date_time DATETIME COMMENT 'Audit timestamp: updated',
    update_by VARCHAR(32) COMMENT 'Updater user id',
    tenant_id VARCHAR(32) COMMENT 'Tenant scope',
    name VARCHAR(255) COMMENT 'Resource name',
    status VARCHAR(32) COMMENT 'AVAILABLE / IN_USE / ARCHIVED / DISABLED',
    description TEXT COMMENT 'Resource description',
    PRIMARY KEY (id),
    KEY idx_resource_name (name),
    KEY idx_resource_status (status)
) COMMENT='Data-dev resource assets (Resource Center)';