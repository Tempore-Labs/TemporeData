-- Flyway migration V18: widen zy_datasource_plugin.id (uuid2 produces 36 chars, >32)
ALTER TABLE zy_datasource_plugin
    MODIFY COLUMN id VARCHAR(64) NOT NULL;