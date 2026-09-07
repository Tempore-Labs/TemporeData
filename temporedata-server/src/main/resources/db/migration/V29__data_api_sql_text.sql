-- V29: DataApi 可承载任意 SQL。`sql` 列原为 varchar(255)，且是未加反引号的保留字 `sql`。
-- 加宽为 LONGTEXT；实体已改用 @Column(name = "`sql`") 显式转义（与 zy_query 同类问题）。
--
-- 修复：zy_data_api 基础表此前从未被任何迁移创建（仅此处被引用），导致全新库
-- 迁移到 V29 时报 1146 Table doesn't exist。在此以 IF NOT EXISTS 幂等方式补建该
-- 表（已存在的旧库自动跳过），列与 DataApiEntity 的 JPA 映射一致。
CREATE TABLE IF NOT EXISTS zy_data_api (
    id                VARCHAR(32)  NOT NULL PRIMARY KEY,
    create_date_time  DATETIME(6),
    create_by         VARCHAR(32),
    update_date_time  DATETIME(6),
    update_by         VARCHAR(32),
    tenant_id         VARCHAR(32),
    name              VARCHAR(128),
    description       TEXT,
    datasource_id     VARCHAR(32),
    `sql`             LONGTEXT NULL,
    method            VARCHAR(8),
    path              VARCHAR(256),
    status            VARCHAR(16),
    api_key           VARCHAR(64),
    create_time       VARCHAR(32),
    update_time       VARCHAR(32)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

ALTER TABLE zy_data_api MODIFY `sql` LONGTEXT NULL;