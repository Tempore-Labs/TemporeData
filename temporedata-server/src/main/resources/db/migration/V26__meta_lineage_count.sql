-- Flyway migration V26 (meta P2-followup): lineage linkage count on table metadata.
ALTER TABLE zy_meta_table ADD COLUMN lineage_count INT DEFAULT 0;