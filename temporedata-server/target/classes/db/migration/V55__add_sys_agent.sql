-- =============================================================================
-- V55: add_sys_agent - AI assistant (light-boat copilot) chat + LLM config
--
-- Purpose:
--   [AI-ASSISTANT] Backs the `sys.agent` module: simulated AI copilot chat
--   messages, session history, and the underlying LLM provider configuration.
--   Previously the table was missing, causing POST /api/agent/chat to fail
--   with a 500 error -> the front-end AI assistant was unusable.
--
-- Notes:
--   1. Pure NEW table -> idempotent CREATE TABLE IF NOT EXISTS.
--   2. Mirrors AgentEntity: 32-char VARCHAR PK, nullable business columns.
--   3. llm=true rows hold LLM provider config (provider/baseUrl/model/...).
--   4. A quick index on session_id supports history lookup ordering.
-- =============================================================================

CREATE TABLE IF NOT EXISTS zy_agent (
    id VARCHAR(36) NOT NULL COMMENT 'Primary key: UUID (matches existing 36-char ids)',
    create_date_time DATETIME COMMENT 'Audit timestamp: created',
    create_by VARCHAR(32) COMMENT 'Creator user id',
    update_date_time DATETIME COMMENT 'Audit timestamp: updated',
    update_by VARCHAR(32) COMMENT 'Updater user id',
    tenant_id VARCHAR(32) COMMENT 'Tenant scope',
    message TEXT COMMENT 'User message text',
    context TEXT COMMENT 'Optional data-object context for the copilot',
    session_id VARCHAR(64) COMMENT 'Chat session id',
    model_name VARCHAR(128) COMMENT 'Model name used for the reply',
    reply TEXT COMMENT 'AI reply text',
    action VARCHAR(32) COMMENT 'user / assistant',
    llm BOOLEAN COMMENT 'Marks an LLM provider config row',
    provider VARCHAR(64) COMMENT 'LLM provider: openai / azure / local ...',
    base_url VARCHAR(512) COMMENT 'LLM API base url',
    api_key VARCHAR(512) COMMENT 'LLM API key (stored encrypted in prod)',
    model VARCHAR(128) COMMENT 'LLM model id',
    temperature DOUBLE COMMENT 'LLM sampling temperature',
    enabled BOOLEAN COMMENT 'Config enabled flag',
    api_key_masked VARCHAR(128) COMMENT 'Masked api key for display',
    api_key_set BOOLEAN COMMENT 'True when an api key is configured',
    title VARCHAR(255) COMMENT 'Session title',
    message_count INTEGER COMMENT 'Message count of a session',
    last_time VARCHAR(64) COMMENT 'Last activity time (formatted)',
    ok BOOLEAN COMMENT 'Success flag of the last generation',
    PRIMARY KEY (id),
    KEY idx_agent_session (session_id)
) COMMENT='AI assistant chat sessions + LLM config (sys.agent)';