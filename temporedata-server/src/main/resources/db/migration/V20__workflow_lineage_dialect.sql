-- Flyway migration V20 (P2): record the parsing dialect on workflow lineage rows
-- so downstream graph/lineage views can filter or annotate by data-source dialect.
-- Idempotent: only adds the column when missing (MySQL has no ADD COLUMN IF NOT EXISTS).
--
-- NOTE: targets `zy_workflow_lineage` (the name present at this migration's run point on a
-- fresh bootstrap, created by V6) rather than `temporedata_workflow_lineage`. The final rename
-- to `temporedata_workflow_lineage` happens later in V31, which carries this column along with
-- the table. Referencing the final name here breaks a clean bootstrap because that table only
-- exists after V31.
SET @tg_sql := IF((SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'zy_workflow_lineage'
                     AND COLUMN_NAME = 'dialect') = 0,
                   'ALTER TABLE zy_workflow_lineage ADD COLUMN dialect VARCHAR(32) DEFAULT NULL',
                   'SELECT 1');
PREPARE tg_stmt FROM @tg_sql; EXECUTE tg_stmt; DEALLOCATE PREPARE tg_stmt;