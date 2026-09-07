-- Flyway migration V10: P3 security & governance (RBAC resources, hash-chain audit, vuln/QA tracking).

-- P3-11: resource catalog + permission matrix.
CREATE TABLE IF NOT EXISTS res_resource (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    resource_type VARCHAR(32) NOT NULL,
    resource_key VARCHAR(128) NOT NULL,
    resource_name VARCHAR(128),
    tenant_id VARCHAR(32),
    owner VARCHAR(32),
    description VARCHAR(255),
    create_time VARCHAR(32),
    UNIQUE KEY uk_res_key (resource_type, resource_key)
);

CREATE TABLE IF NOT EXISTS res_permission (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    role_id VARCHAR(36) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    resource_key VARCHAR(128) DEFAULT '*',
    action VARCHAR(16) NOT NULL,
    scope VARCHAR(8) NOT NULL DEFAULT 'ALLOW',
    tenant_id VARCHAR(32),
    create_time VARCHAR(32)
);

-- P3-12: unified financial-grade audit with tamper-evident hash chain.
CREATE TABLE IF NOT EXISTS audit_event (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    event_time VARCHAR(32),
    tenant_id VARCHAR(32),
    operator VARCHAR(32),
    ip VARCHAR(64),
    action VARCHAR(64),
    resource_type VARCHAR(32),
    resource_key VARCHAR(128),
    detail_json TEXT,
    module VARCHAR(32),
    req_id VARCHAR(64),
    status VARCHAR(16),
    seq BIGINT,
    prev_hash VARCHAR(64),
    event_hash VARCHAR(64),
    KEY idx_ae_seq (seq)
);

CREATE TABLE IF NOT EXISTS audit_policy (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    module VARCHAR(32),
    action VARCHAR(64),
    audit_level VARCHAR(8) DEFAULT 'FULL',
    retention_days INT DEFAULT 180,
    notify_alert TINYINT(1) DEFAULT 0
);

CREATE TABLE IF NOT EXISTS audit_archive (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    period_start VARCHAR(32),
    period_end VARCHAR(32),
    record_count INT,
    root_hash VARCHAR(64),
    signed_by VARCHAR(64),
    archive_path VARCHAR(255),
    create_time VARCHAR(32)
);

-- P3-13: vulnerability tracker + QA report.
CREATE TABLE IF NOT EXISTS vul_tracker (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    source VARCHAR(32),
    package_name VARCHAR(128),
    cve VARCHAR(64),
    cvss DECIMAL(4,2),
    severity VARCHAR(16),
    affected_ver VARCHAR(64),
    fixed_ver VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    pr_ref VARCHAR(128),
    mitigation VARCHAR(255),
    report_time VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS qa_report (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    version VARCHAR(32),
    module VARCHAR(64),
    kind VARCHAR(32),
    summary TEXT,
    coverage DECIMAL(5,2),
    gate VARCHAR(16),
    run_time VARCHAR(32)
);