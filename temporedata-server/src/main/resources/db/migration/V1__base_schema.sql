-- Flyway migration V1: Base schema for temporedata

-- Tenant table
CREATE TABLE IF NOT EXISTS zy_tenant (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User table
CREATE TABLE IF NOT EXISTS zy_user (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    real_name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    account_expired_at TIMESTAMP,
    tenant_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Role table
CREATE TABLE IF NOT EXISTS zy_role (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

-- User-Role mapping
CREATE TABLE IF NOT EXISTS zy_user_role (
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- Datasource table
CREATE TABLE IF NOT EXISTS zy_datasource (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    host VARCHAR(200),
    port INT,
    database_name VARCHAR(100),
    username VARCHAR(100),
    password VARCHAR(500),
    params TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    tenant_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Workflow table
CREATE TABLE IF NOT EXISTS zy_workflow (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT',
    cron_expression VARCHAR(100),
    schedule_enabled TINYINT(1) DEFAULT 0,
    tenant_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Workflow node table
CREATE TABLE IF NOT EXISTS zy_workflow_node (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    script TEXT,
    config TEXT,
    position_x DOUBLE DEFAULT 0,
    position_y DOUBLE DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Workflow edge table
CREATE TABLE IF NOT EXISTS zy_workflow_edge (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL,
    source_node_id VARCHAR(36) NOT NULL,
    target_node_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Workflow execution table
CREATE TABLE IF NOT EXISTS zy_workflow_execute (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    workflow_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Workflow node execution table
CREATE TABLE IF NOT EXISTS zy_workflow_node_execute (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    execution_id VARCHAR(36) NOT NULL,
    node_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration_ms BIGINT,
    log_text TEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default roles
INSERT INTO zy_role (id, code, name) VALUES ('role-admin', 'ROLE_ADMIN', 'Admin') ON DUPLICATE KEY UPDATE name = name;
INSERT INTO zy_role (id, code, name) VALUES ('role-tenant-admin', 'ROLE_TENANT_ADMIN', 'Tenant Admin') ON DUPLICATE KEY UPDATE name = name;
INSERT INTO zy_role (id, code, name) VALUES ('role-member', 'ROLE_TENANT_MEMBER', 'Member') ON DUPLICATE KEY UPDATE name = name;