-- Flyway migration V22 (P1): persistent desensitization rules (MaskRule).
CREATE TABLE IF NOT EXISTS zy_mask_rule (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(128),
    rule_type VARCHAR(32) NOT NULL,            -- PHONE/EMAIL/NAME/ID_CARD/BANK_CARD/CUSTOM/HASH/DROP
    mask_pattern VARCHAR(200),
    description VARCHAR(500),
    datasource_id VARCHAR(64),
    table_name VARCHAR(200),
    column_name VARCHAR(200),
    status INT NOT NULL DEFAULT 1,              -- 1=enabled, 0=disabled
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_mask_ds (datasource_id, table_name, status)
);