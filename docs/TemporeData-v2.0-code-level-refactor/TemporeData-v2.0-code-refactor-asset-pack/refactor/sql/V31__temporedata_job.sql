CREATE TABLE temporedata_job (
    id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    definition_json JSON NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    version_no BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_job_tenant_status (tenant_id, status),
    UNIQUE KEY uk_job_tenant_name (tenant_id, name)
);

CREATE TABLE temporedata_job_execution (
    id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    job_id VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    executor_type VARCHAR(64) NOT NULL,
    trace_id VARCHAR(128),
    started_at DATETIME,
    finished_at DATETIME,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_job_exec_job_created (tenant_id, job_id, created_at),
    KEY idx_job_exec_status_created (tenant_id, status, created_at)
);
