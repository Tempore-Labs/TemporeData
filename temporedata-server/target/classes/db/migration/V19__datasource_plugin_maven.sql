-- Flyway migration V19: Maven GAV coordinates for driver plugins
-- Backs "上传驱动：Maven 坐标动态解析依赖". mvn_group/artifact/version let the
-- server resolve the driver and its transitive dependencies from a Maven repo.
ALTER TABLE zy_datasource_plugin
    ADD COLUMN mvn_group    VARCHAR(128) DEFAULT NULL,
    ADD COLUMN mvn_artifact VARCHAR(128) DEFAULT NULL,
    ADD COLUMN mvn_version  VARCHAR(50)  DEFAULT NULL;