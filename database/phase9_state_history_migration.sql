-- Allow an accepted contractor update to append the outgoing project state,
-- including multiple state changes on the same calendar day.
USE mplad_db;

CREATE TABLE IF NOT EXISTS project_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    project_id BIGINT UNSIGNED NOT NULL,
    update_date DATE NOT NULL,
    completion_percentage DECIMAL(5, 2) UNSIGNED NOT NULL,
    expenditure DECIMAL(15, 2) UNSIGNED NOT NULL,
    delay_days INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(40) NOT NULL,
    indicator_score DECIMAL(5, 2) NULL,
    indicator_level VARCHAR(20) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_project_history_project_date (project_id, update_date),
    CONSTRAINT fk_project_history_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT chk_project_history_completion CHECK (completion_percentage <= 100.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @mplad_history_unique_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'project_history'
      AND index_name = 'uq_project_history_update'
);
SET @mplad_history_index_ddl = IF(
    @mplad_history_unique_index_exists > 0,
    'ALTER TABLE project_history DROP INDEX uq_project_history_update',
    'SELECT 1'
);
PREPARE mplad_history_index_stmt FROM @mplad_history_index_ddl;
EXECUTE mplad_history_index_stmt;
DEALLOCATE PREPARE mplad_history_index_stmt;

SET @mplad_project_score_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'projects' AND column_name = 'last_analysis_score'
);
SET @mplad_project_score_ddl = IF(@mplad_project_score_exists = 0,
    'ALTER TABLE projects ADD COLUMN last_analysis_score DECIMAL(5, 2) NULL', 'SELECT 1');
PREPARE mplad_project_score_stmt FROM @mplad_project_score_ddl;
EXECUTE mplad_project_score_stmt;
DEALLOCATE PREPARE mplad_project_score_stmt;

SET @mplad_project_level_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'projects' AND column_name = 'last_analysis_level'
);
SET @mplad_project_level_ddl = IF(@mplad_project_level_exists = 0,
    'ALTER TABLE projects ADD COLUMN last_analysis_level VARCHAR(20) NULL', 'SELECT 1');
PREPARE mplad_project_level_stmt FROM @mplad_project_level_ddl;
EXECUTE mplad_project_level_stmt;
DEALLOCATE PREPARE mplad_project_level_stmt;

SET @mplad_history_score_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'project_history' AND column_name = 'indicator_score'
);
SET @mplad_history_score_ddl = IF(@mplad_history_score_exists = 0,
    'ALTER TABLE project_history ADD COLUMN indicator_score DECIMAL(5, 2) NULL', 'SELECT 1');
PREPARE mplad_history_score_stmt FROM @mplad_history_score_ddl;
EXECUTE mplad_history_score_stmt;
DEALLOCATE PREPARE mplad_history_score_stmt;

SET @mplad_history_level_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'project_history' AND column_name = 'indicator_level'
);
SET @mplad_history_level_ddl = IF(@mplad_history_level_exists = 0,
    'ALTER TABLE project_history ADD COLUMN indicator_level VARCHAR(20) NULL', 'SELECT 1');
PREPARE mplad_history_level_stmt FROM @mplad_history_level_ddl;
EXECUTE mplad_history_level_stmt;
DEALLOCATE PREPARE mplad_history_level_stmt;
