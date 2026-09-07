-- V33: give lineage PK columns headroom for the JPA uuid generator.
-- The @GenericGenerator(strategy="uuid2") on LineageEntity may emit a 36-char UUID,
-- but temporedata_lineage.id is varchar(32), so an enriched-edge insert can fail with
-- "Data too long for column 'id'" (Error 1406). Widen the PK (and the FQN-ish id columns
-- used as lineage node references) so both the assigned (24-hex) and generated (36-char)
-- identifiers fit.
ALTER TABLE temporedata_lineage MODIFY id VARCHAR(64) NOT NULL;
ALTER TABLE temporedata_column_lineage MODIFY id VARCHAR(64) NOT NULL;
ALTER TABLE temporedata_workflow_lineage MODIFY id VARCHAR(64) NOT NULL;
ALTER TABLE temporedata_meta_table MODIFY id VARCHAR(64) NOT NULL;