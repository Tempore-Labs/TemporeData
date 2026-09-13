-- =============================================================================
-- V50: add_asset_and_metric - Asset Catalog & Metric Platform
--
-- Purpose:
--   [ENT-P0] Introduces two tables:
--     * temporedata_asset_catalog  - unified asset view over a dataset (business
--       term / tags / popularity / owner) for the asset marketplace & lineage embed.
--     * temporedata_metric         - metric definitions: atomic / derived / compound,
--       with definition SQL and publication to SQL/REST API.
--
-- Notes:
--   1. Pure NEW tables -> idempotent CREATE TABLE IF NOT EXISTS.
--   2. asset_catalog.dataset_id is UNIQUE -> one catalog entry per dataset (V47).
--   3. metric.code is the unique business key; metric_type is ATOMIC/DERIVED/COMPOUND.
-- =============================================================================

CREATE TABLE IF NOT EXISTS temporedata_asset_catalog (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dataset_id BIGINT NOT NULL COMMENT 'FK -> temporedata_dataset.id (one catalog per dataset)',
    business_term VARCHAR(256) COMMENT 'Bound business glossary term',
    tags VARCHAR(512) COMMENT 'Comma-separated asset tags',
    popularity INT DEFAULT 0 COMMENT 'Asset heat / hit count',
    owner VARCHAR(64) COMMENT 'Data owner account',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit timestamp: created',
    updated_at DATETIME COMMENT 'Audit timestamp: updated',
    PRIMARY KEY (id),
    UNIQUE KEY uk_asset_dataset (dataset_id),
    KEY idx_asset_tags (tags),
    KEY idx_asset_popularity (popularity)
) COMMENT='Unified asset catalog (Enterprise P0)';

CREATE TABLE IF NOT EXISTS temporedata_metric (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL COMMENT 'Unique metric business code',
    name VARCHAR(128) NOT NULL COMMENT 'Metric display name',
    metric_type VARCHAR(32) COMMENT 'ATOMIC / DERIVED / COMPOUND',
    dataset_id BIGINT COMMENT 'FK -> temporedata_dataset.id',
    definition_sql TEXT COMMENT 'Metric definition / caliber SQL',
    owner VARCHAR(64) COMMENT 'Metric owner account',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit timestamp: created',
    updated_at DATETIME COMMENT 'Audit timestamp: updated',
    PRIMARY KEY (id),
    UNIQUE KEY uk_metric_code (code),
    KEY idx_metric_dataset (dataset_id)
) COMMENT='Metric platform definitions (Enterprise P0)';