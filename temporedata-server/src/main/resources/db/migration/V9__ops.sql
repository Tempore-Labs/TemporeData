-- Flyway migration V9: P2-10 GitHub/GitLab Ops integration (repos + builds).

CREATE TABLE IF NOT EXISTS ops_repo (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    repo_ref VARCHAR(200) NOT NULL,
    branch VARCHAR(64) DEFAULT 'main',
    auto_trigger TINYINT(1) NOT NULL DEFAULT 0,
    deploy_script_path VARCHAR(255),
    environment VARCHAR(32) DEFAULT 'DEV',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    bind_info_json TEXT,
    create_time VARCHAR(32),
    update_time VARCHAR(32),
    UNIQUE KEY uk_ops_repo (provider, repo_ref)
);

CREATE TABLE IF NOT EXISTS ops_build (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    repo_id VARCHAR(36) NOT NULL,
    trigger_type VARCHAR(16),
    ref_name VARCHAR(64),
    commit_sha VARCHAR(64),
    commit_message VARCHAR(500),
    status VARCHAR(16) NOT NULL DEFAULT 'TRIGGERED',
    pipeline_ref VARCHAR(128),
    artifact_id VARCHAR(36),
    log_ref VARCHAR(64),
    create_time VARCHAR(32),
    update_time VARCHAR(32)
);