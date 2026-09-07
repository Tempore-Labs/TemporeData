CREATE TABLE temporedata_cluster (
    id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    endpoint VARCHAR(512),
    version VARCHAR(64),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    version_no BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY idx_cluster_tenant_status (tenant_id, status),
    UNIQUE KEY uk_cluster_tenant_name (tenant_id, name)
);
