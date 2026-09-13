-- =============================================================================
-- V47: add_unified_dataset - Unified top-level dataset abstraction
--
-- Purpose:
--   [ENT-P0] Introduces `temporedata_dataset`, the cross-domain linkage key that
--   metadata / modeling / quality / lineage / contract / security all hang off.
--   Mirrors the DatasetDTO contract in temporedata-api (org.temporedata.api.dataset).
--
-- Notes:
--   1. Pure NEW table -> idempotent CREATE TABLE IF NOT EXISTS; safe to re-run.
--   2. DB schema is already at v46, so this migration applies cleanly on top.
--   3. Idempotency / version-control conventions follow the existing V31-V33 style
--      (no engine/charset override; inherits DB-wide utf8mb4 default).
-- =============================================================================

CREATE TABLE IF NOT EXISTS temporedata_dataset (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL COMMENT 'Display name',
    code VARCHAR(128) NOT NULL COMMENT 'Unique business code (e.g. dws_sales_daily)',
    domain_id BIGINT COMMENT 'DataDomain id this dataset belongs to',
    layer VARCHAR(16) COMMENT 'Warehouse layer: ODS / DWD / DWS / ADS',
    security_level VARCHAR(32) COMMENT 'Security classification level',
    owner VARCHAR(64) COMMENT 'Data owner account',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit timestamp: created',
    updated_at DATETIME COMMENT 'Audit timestamp: updated',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dataset_code (code),
    KEY idx_dataset_domain (domain_id),
    KEY idx_dataset_layer (layer)
) COMMENT='Unified dataset top-level object (Enterprise P0)';