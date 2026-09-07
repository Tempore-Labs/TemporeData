-- Flyway migration V11: integration secret store (zy_secret)
-- Backs the "密钥管理" (Secret) feature. Columns mirror SecretEntity.

CREATE TABLE IF NOT EXISTS zy_secret (
    id                 VARCHAR(32)  NOT NULL PRIMARY KEY,
    create_date_time   DATETIME(6),
    create_by          VARCHAR(32),
    update_date_time   DATETIME(6),
    update_by          VARCHAR(32),
    tenant_id          VARCHAR(32),
    `key`              VARCHAR(255),
    value              LONGTEXT,
    description        VARCHAR(255),
    scope              VARCHAR(32),
    create_time        VARCHAR(40)
) DEFAULT CHARSET = utf8mb4;