-- =============================================================================
-- V48: add_metadata_drift - Metadata versioning & Schema Drift
--
-- Purpose:
--   [ENT-P0] Introduces `temporedata_metadata_version`: one row per schema snapshot
--   of a dataset. Enables Schema Crawler versioning and Schema Drift detection
--   (ADD_COLUMN / DROP_COLUMN / TYPE_CHANGE) with impact analysis.
--
-- Notes:
--   1. Pure NEW table -> idempotent CREATE TABLE IF NOT EXISTS.
--   2. dataset_id links back to temporedata_dataset (V47).
--   3. Natural identity (dataset_id, version) is unique; query by dataset+created_at.
-- =============================================================================

CREATE TABLE IF NOT EXISTS temporedata_metadata_version (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dataset_id BIGINT NOT NULL COMMENT 'FK -> temporedata_dataset.id',
    version INT NOT NULL COMMENT 'Monotonic schema version for the dataset',
    schema_json TEXT NOT NULL COMMENT 'Snapshot: columns, types, DDL',
    drift_type VARCHAR(32) COMMENT 'ADD_COLUMN / DROP_COLUMN / TYPE_CHANGE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit timestamp: created',
    PRIMARY KEY (id),
    UNIQUE KEY uk_metaver_dataset_version (dataset_id, version),
    KEY idx_metaver_dataset_created (dataset_id, created_at)
) COMMENT='Metadata schema snapshot & drift versioning (Enterprise P0)';