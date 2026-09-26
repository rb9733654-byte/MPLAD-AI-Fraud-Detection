-- Idempotent state-label migration for an existing MPLAD demo database.
-- Preserves project rows and values already populated in state.
USE mplad_db;

SET @mplad_state_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'projects'
      AND column_name = 'state'
);
SET @mplad_ddl = IF(
    @mplad_state_exists = 0,
    'ALTER TABLE projects ADD COLUMN state VARCHAR(120) NULL AFTER district',
    'SELECT 1'
);
PREPARE mplad_migration_stmt FROM @mplad_ddl;
EXECUTE mplad_migration_stmt;
DEALLOCATE PREPARE mplad_migration_stmt;

UPDATE projects
SET state = CASE project_id
    WHEN 'DEMO-MPL-1001' THEN 'Demo State North'
    WHEN 'DEMO-MPL-1002' THEN 'Demo State North'
    WHEN 'DEMO-MPL-1003' THEN 'Demo State East'
    WHEN 'DEMO-MPL-1004' THEN 'Demo State West'
    WHEN 'DEMO-MPL-1005' THEN 'Demo State East'
    WHEN 'DEMO-MPL-1006' THEN 'Demo State South'
    WHEN 'DEMO-MPL-1007' THEN 'Demo State Central'
    WHEN 'DEMO-MPL-1008' THEN 'Demo State South'
    WHEN 'DEMO-MPL-1009' THEN 'Demo State Central'
    WHEN 'DEMO-MPL-1010' THEN 'Demo State West'
    ELSE 'Unspecified demo state'
END
WHERE state IS NULL OR state = '';

ALTER TABLE projects MODIFY COLUMN state VARCHAR(120) NOT NULL;
