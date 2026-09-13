-- =============================================================================
-- V51: add_data_contract - Declarative Data Contract
--
-- Purpose:
--   [ENT-P1] Introduces `temporedata_data_contract`: one declarative contract per
--   dataset (YAML/JSON: schema, SLA latency commitment, quality requirements) plus
--   the compatibility mode & lifecycle status used by Contract Validation on DDL
--   change or publish.
--
-- Notes:
--   1. Pure NEW table -> idempotent CREATE TABLE IF NOT EXISTS.
--   2. dataset_id is UNIQUE -> one active contract per dataset (V47); a new revision
--      supersedes via status UPDATE rather than an extra row.
--   3. compatibility_mode is BACKWARD / FULL; status drives Producer/Consumer gating.
-- =============================================================================

CREATE TABLE IF NOT EXISTS temporedata_data_contract (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dataset_id BIGINT NOT NULL COMMENT 'FK -> temporedata_dataset.id (one contract per dataset)',
    contract_yaml TEXT NOT NULL COMMENT 'Declared contract YAML/JSON (schema &amp; SLA &amp; quality)',
    compatibility_mode VARCHAR(32) COMMENT 'BACKWARD / FULL',
    status VARCHAR(32) COMMENT 'DRAFT / ACTIVE / BROKEN / SUPERSEDED',
    owner VARCHAR(64) COMMENT 'Contract owner account',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit timestamp: created',
    updated_at DATETIME COMMENT 'Audit timestamp: updated',
    PRIMARY KEY (id),
    UNIQUE KEY uk_contract_dataset (dataset_id),
    KEY idx_contract_status (status)
) COMMENT='Declarative data contract (Enterprise P1)';