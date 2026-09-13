-- Fix: remaining uuid2 entities whose id tables were still VARCHAR(32) and were
-- missed by V62 (DependencyEntity / GlobalvarEntity / HaEntity / MonitorEntity /
-- ResourceEntity). Their 36-char ids failed inserts with "Data too long". Idempotent.
ALTER TABLE zy_dependency MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_globalvar  MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_ha         MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_monitor    MODIFY id VARCHAR(36) NOT NULL;
ALTER TABLE zy_resource   MODIFY id VARCHAR(36) NOT NULL;