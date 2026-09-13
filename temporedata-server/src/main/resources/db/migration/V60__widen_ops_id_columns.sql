-- Fix: Hibernate uuid2 ids are 36 chars; widen id columns that were created at 32.
ALTER TABLE zy_sync MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_ingestion MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_engine MODIFY id VARCHAR(36) NOT NULL;