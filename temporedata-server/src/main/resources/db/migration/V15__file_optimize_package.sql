-- Flyway migration V15: image-optimization & small-file packaging metadata
ALTER TABLE zy_file
    ADD COLUMN image_optimized TINYINT(1) DEFAULT 0 COMMENT '是否已进行图片优化(有损)',
    ADD COLUMN packaged       TINYINT(1) DEFAULT 0 COMMENT '是否为打包归档文件',
    ADD COLUMN member_count   INT         DEFAULT 0 COMMENT '打包成员数';