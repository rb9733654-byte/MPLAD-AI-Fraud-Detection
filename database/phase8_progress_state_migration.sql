-- Store whether each historical submission was trusted to update current project state.
-- Existing evidence is preserved; the backfill below accepts only valid matched entries.
USE mplad_db;

SET @mplad_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'progress_evidence'
      AND column_name = 'accepted_for_project_state'
);
SET @mplad_ddl = IF(@mplad_column_exists = 0,
    'ALTER TABLE progress_evidence ADD COLUMN accepted_for_project_state TINYINT(1) NOT NULL DEFAULT 0',
    'SELECT 1');
PREPARE mplad_migration_stmt FROM @mplad_ddl;
EXECUTE mplad_migration_stmt;
DEALLOCATE PREPARE mplad_migration_stmt;

-- Backfill the current project record once from the latest historically valid
-- evidence. A clear location mismatch or an out-of-range value is not accepted.
-- Re-running this migration is safe: it only reapplies the latest accepted
-- evidence, and does not remove or truncate any evidence or project rows.
UPDATE progress_evidence e
JOIN projects p ON p.id = e.project_id
SET e.accepted_for_project_state = 1
WHERE e.verification_status = 'Location Matched'
  AND (e.completion_percentage IS NULL OR e.completion_percentage BETWEEN 0 AND 100)
  AND (e.expenditure IS NULL OR (e.expenditure >= 0 AND e.expenditure <= p.released_amount))
  AND (e.completion_percentage IS NOT NULL OR e.expenditure IS NOT NULL);

UPDATE projects p
JOIN (
    SELECT e.project_id, e.completion_percentage, e.expenditure
    FROM progress_evidence e
    JOIN (
        SELECT project_id, MAX(id) AS evidence_id
        FROM progress_evidence
        WHERE accepted_for_project_state = 1
        GROUP BY project_id
    ) latest ON latest.project_id = e.project_id AND latest.evidence_id = e.id
) accepted ON accepted.project_id = p.id
SET p.completion_percentage = COALESCE(accepted.completion_percentage, p.completion_percentage),
    p.actual_expenditure = COALESCE(accepted.expenditure, p.actual_expenditure),
    p.is_completed = CASE WHEN COALESCE(accepted.completion_percentage, p.completion_percentage) >= 100 THEN 1 ELSE 0 END,
    p.expenditure_per_completion_percent = CASE
        WHEN COALESCE(accepted.completion_percentage, p.completion_percentage) <= 0 THEN 0
        ELSE ROUND(COALESCE(accepted.expenditure, p.actual_expenditure)
                   / COALESCE(accepted.completion_percentage, p.completion_percentage), 4)
    END;
