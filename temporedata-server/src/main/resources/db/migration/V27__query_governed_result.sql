-- meta P0/数据访问控制：真实查询执行结果表 zy_query。
-- 查询结果（列 + 治理后行）落库为 JSON，供历史回看；暂无既有表，直接建。
CREATE TABLE IF NOT EXISTS zy_query (
    id                VARCHAR(32)  NOT NULL PRIMARY KEY,
    create_date_time  DATETIME(6),
    create_by         VARCHAR(32),
    update_date_time  DATETIME(6),
    update_by         VARCHAR(32),
    tenant_id         VARCHAR(32),
    datasource_name   VARCHAR(255),
    sql_text          TEXT,
    status            VARCHAR(255),
    duration_ms       BIGINT,
    create_time       VARCHAR(255),
    error_msg         TEXT,
    datasource_id     VARCHAR(255),
    row_count         INT,
    columns_json      TEXT,
    rows_json         TEXT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;