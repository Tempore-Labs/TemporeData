-- Flyway migration V16: keep-original key for image optimization
ALTER TABLE zy_file
    ADD COLUMN original_key VARCHAR(255) NULL COMMENT '图片优化时保留的原图对象 key(用于保持原图下载)';