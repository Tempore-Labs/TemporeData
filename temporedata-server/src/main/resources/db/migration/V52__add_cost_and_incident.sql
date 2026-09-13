-- =============================================================================
-- V52: add_cost_and_incident - FinOps cost ledger & Incident/RCA (DataOps)
--
-- Purpose:
--   [ENT-P2] Introduces two tables for the DataOps / FinOps control plane:
--     * temporedata_data_cost   - per-dataset compute/storage/query cost ledger used
--       by the temporedata-service `cost` package for billing & cost-reduction advice.
--     * temporedata_incident    - incident events with severity/status and root-cause
--       analysis text, feeding the temporedata-service `incident`/RCA engine and the
--       Quality Issue -> Incident -> RCA Agent -> Action Plan loop.
--
-- Notes:
--   1. Pure NEW tables -> idempotent CREATE TABLE IF NOT EXISTS.
--   2. dataset_id links to temporedata_dataset (V47); incident.dataset_id is nullable
--      because an incident may originate outside a single dataset (job / API / infra).
--   3. Unique (dataset_id, record_date) keeps one cost aggregate per dataset per day.
-- =============================================================================

CREATE TABLE IF NOT EXISTS temporedata_data_cost (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dataset_id BIGINT NOT NULL COMMENT 'FK -> temporedata_dataset.id',
    compute_cost DECIMAL(10,2) COMMENT 'Computation (CPU/query) cost for the day',
    storage_cost DECIMAL(10,2) COMMENT 'Storage cost for the day',
    query_cost DECIMAL(10,2) COMMENT 'Pay-per-query cost for the day',
    record_date DATE NOT NULL COMMENT 'Cost day',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit timestamp: created',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cost_dataset_date (dataset_id, record_date),
    KEY idx_cost_date (record_date)
) COMMENT='FinOps cost ledger per dataset per day (Enterprise P2)';

CREATE TABLE IF NOT EXISTS temporedata_incident (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(256) NOT NULL COMMENT 'Incident title',
    source_type VARCHAR(32) COMMENT 'QUALITY / ALARM / WORKFLOW / API / INFRA / AI_ACTION',
    dataset_id BIGINT COMMENT 'FK -> temporedata_dataset.id (nullable)',
    severity VARCHAR(16) COMMENT 'CRITICAL / HIGH / MEDIUM / LOW',
    status VARCHAR(32) COMMENT 'OPEN / INVESTIGATING / RESOLVED / CLOSED',
    rca_analysis TEXT COMMENT 'Root-cause analysis (RCA) text',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Audit timestamp: created',
    updated_at DATETIME COMMENT 'Audit timestamp: updated',
    PRIMARY KEY (id),
    KEY idx_incident_status (status),
    KEY idx_incident_severity (severity),
    KEY idx_incident_dataset (dataset_id)
) COMMENT='Incident & RCA engine events (Enterprise P2)';