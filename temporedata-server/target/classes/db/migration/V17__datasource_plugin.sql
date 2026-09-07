-- Flyway migration V17: datasource plugin registry (uploaded external JDBC driver plugins)
-- Backs the "外部数据源插件"(datasource plugin) feature: uploaded jar plugins that are
-- isolated-loaded via a URLClassLoader and registered into DatasourcePluginContext.

CREATE TABLE IF NOT EXISTS zy_datasource_plugin
(
    id           VARCHAR(32)  NOT NULL,
    db_type      VARCHAR(32)  NOT NULL COMMENT '数据库类型,唯一键(大写)',
    name         VARCHAR(100) DEFAULT NULL COMMENT '插件名称',
    version      VARCHAR(50)  DEFAULT NULL COMMENT '插件/驱动版本',
    driver_class VARCHAR(255) DEFAULT NULL COMMENT '驱动类全名(如 oracle.jdbc.OracleDriver)',
    driver_path  VARCHAR(500) DEFAULT NULL COMMENT '上传驱动 jar 在服务端的路径',
    status       VARCHAR(20)  DEFAULT 'ENABLED' COMMENT 'ENABLED|DISABLED',
    builtin      TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否内置插件(1=内置 0=上传)',
    tenant_id    VARCHAR(32)  DEFAULT NULL,
    create_by    VARCHAR(32)  DEFAULT NULL,
    create_time  DATETIME     DEFAULT NULL,
    update_time  DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_ds_plugin_dbtype (db_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='数据源插件注册表(上传的 JDBC 驱动插件)';