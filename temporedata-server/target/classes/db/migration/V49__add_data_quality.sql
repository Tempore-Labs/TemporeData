-- =============================================================================
-- V49: add_data_quality - Quality rule definitions & Quality Gates
--
-- Purpose:
--   [ENT-P0] Introduces two tables:
--     * temporedata_quality_rule            - multi-dimension validation rule definition
--       (Completeness / Uniqueness / Validity / Accuracy / Timeliness).
--     * temporedata_quality_gate            - gate config that couples with the
--       WorkflowEngine to auto-block downstream publishing when score < threshold.
--
-- Notes:
--   1. Pure NEW tables -> idempotent CREATE TABLE IF NOT EXISTS.
--   2. dataset_id links back to temporedata_dataset (V47).
--   3. BOOLEAN stored as TINYINT(1); threshold_score scaled to 2 decimals (0-100).
-- =============================================================================

CREATE TABLE IF NOT EXISTS temporedata_quality_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dataset_id BIGINT NOT NULL COMMENT 'FK -> temporedata_dataset.id',
    dimension VARCHAR(32) COMMENT 'COMPLETENESS / UNIQUENESS / VALIDITY / ACCURACY / TIMELINESS',
    rule_type VARCHAR(64) COMMENT 'Rule family (e.g. NOT_NULL_RATE, UNIQUE_RATE, ...)',
    expression TEXT COMMENT 'Rule expression / SQL predicate',
    threshold_score DECIMAL(5,2) COMMENT 'Fail threshold score (0-100)',
    is_blocking TINYINT(1) DEFAULT 0 COMMENT 'Whether a violation blocks downstream',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit timestamp: created',
    updated_at DATETIME COMMENT 'Audit timestamp: updated',
    PRIMARY KEY (id),
    KEY idx_qrule_dataset (dataset_id)
) COMMENT='Data quality rule definitions (Enterprise P0)';

CREATE TABLE IF NOT EXISTS temporedata_quality_gate (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dataset_id BIGINT NOT NULL COMMENT 'FK -> temporedata_dataset.id',
    rule_id BIGINT COMMENT 'FK -> temporedata_quality_rule.id',
    min_score DECIMAL(5,2) COMMENT 'Lower bound; below it the gate blocks',
    status VARCHAR(32) COMMENT 'PASSED / BLOCKED / SKIPPED',
    blocking TINYINT(1) DEFAULT 0 COMMENT 'Whether this gate enforces a hard block',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit timestamp: created',
    updated_at DATETIME COMMENT 'Audit timestamp: updated',
    PRIMARY KEY (id),
    UNIQUE KEY uk_qgate_dataset_rule (dataset_id, rule_id),
    KEY idx_qgate_dataset (dataset_id)
) COMMENT='Quality gate config coupled with WorkflowEngine (Enterprise P0)';