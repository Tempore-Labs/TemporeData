-- Fix: Hibernate uuid2 ids are 36 chars; zy_alarm.id was created at 32 and
-- failed inserts with "Data too long" (same bug class as V60). Widen to 36.
ALTER TABLE zy_alarm MODIFY id VARCHAR(36) NOT NULL;