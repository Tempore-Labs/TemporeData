-- Flyway migration V14: transparent file compression metadata (zy_file)
-- Backs the "文件上传压缩" feature: store whether a file was gzip-compressed
-- and its original/stored sizes for storage accounting.

ALTER TABLE zy_file
    ADD COLUMN compressed    TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否已透明压缩存储(0=原始 1=gzip)',
    ADD COLUMN compress_algo VARCHAR(16) DEFAULT 'NONE' COMMENT 'none|gzip|zstd',
    ADD COLUMN original_size BIGINT      NULL COMMENT '原始字节(用户视角 size)',
    ADD COLUMN stored_size   BIGINT      NULL COMMENT '存储字节(压缩后真实占用)';