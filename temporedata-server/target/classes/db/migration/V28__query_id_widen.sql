-- query governed result：id 列需容纳 Hibernate uuid2 生成的 36 字符 UUID，
-- 与库内其它表（zy_datasource.id 等 varchar(255)）保持一致。V27 已应用，故用追加迁移修正。
ALTER TABLE zy_query MODIFY id VARCHAR(255) NOT NULL;