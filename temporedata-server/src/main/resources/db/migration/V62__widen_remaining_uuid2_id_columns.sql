-- Fix: remaining Hibernate uuid2 entities (36-char ids) mapped to tables whose id
-- columns were still VARCHAR(32), causing "Data too long" on insert (same class as
-- V60/V61). Widen id to 36. Idempotent.
ALTER TABLE zy_apilog        MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_blacklist     MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_data_api      MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_datacenter    MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_form          MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_func          MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_indicator     MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_mydata        MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_org           MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_permapproval  MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_quality       MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_report        MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_secret        MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_sensitive     MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_tag           MODIFY id VARCHAR(36) NOT NULL;