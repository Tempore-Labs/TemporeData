-- P1: workflow scheduling policy / misfire, and runtime control support.
ALTER TABLE zy_workflow
    ADD COLUMN schedule_policy VARCHAR(16) NULL COMMENT 'SERIAL | PARALLEL | DROP' AFTER schedule_enabled,
    ADD COLUMN schedule_missfire VARCHAR(16) NULL COMMENT 'SKIP | BACKFILL' AFTER schedule_policy;