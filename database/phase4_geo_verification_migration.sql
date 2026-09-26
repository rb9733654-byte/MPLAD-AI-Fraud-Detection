-- Idempotent contractor and geo-verification migration for MPLAD demo data.
-- This script never drops tables or project rows. Existing non-null values are kept.
USE mplad_db;

SET @mplad_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'projects' AND column_name = 'contractor_id'
);
SET @mplad_ddl = IF(@mplad_column_exists = 0,
    'ALTER TABLE projects ADD COLUMN contractor_id VARCHAR(50) NULL', 'SELECT 1');
PREPARE mplad_migration_stmt FROM @mplad_ddl;
EXECUTE mplad_migration_stmt;
DEALLOCATE PREPARE mplad_migration_stmt;

SET @mplad_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'projects' AND column_name = 'latitude'
);
SET @mplad_ddl = IF(@mplad_column_exists = 0,
    'ALTER TABLE projects ADD COLUMN latitude DECIMAL(10,7) NULL', 'SELECT 1');
PREPARE mplad_migration_stmt FROM @mplad_ddl;
EXECUTE mplad_migration_stmt;
DEALLOCATE PREPARE mplad_migration_stmt;

SET @mplad_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'projects' AND column_name = 'longitude'
);
SET @mplad_ddl = IF(@mplad_column_exists = 0,
    'ALTER TABLE projects ADD COLUMN longitude DECIMAL(10,7) NULL', 'SELECT 1');
PREPARE mplad_migration_stmt FROM @mplad_ddl;
EXECUTE mplad_migration_stmt;
DEALLOCATE PREPARE mplad_migration_stmt;

-- Assign the existing fictional contractor IDs only to the known demo projects.
UPDATE projects
SET contractor_id = CASE
    WHEN project_id IN ('DEMO-MPL-1001', 'DEMO-MPL-1002') THEN 'DEMO-CONTRACTOR-01'
    ELSE 'DEMO-CONTRACTOR-02'
END
WHERE (contractor_id IS NULL OR contractor_id = '')
  AND project_id IN (
      'DEMO-MPL-1001', 'DEMO-MPL-1002', 'DEMO-MPL-1003', 'DEMO-MPL-1004',
      'DEMO-MPL-1005', 'DEMO-MPL-1006', 'DEMO-MPL-1007', 'DEMO-MPL-1008',
      'DEMO-MPL-1009', 'DEMO-MPL-1010'
  );

-- These ten records are fictional demo data. Use the AEC campus test point so
-- contractor GPS submissions from campus can exercise the unchanged proximity
-- verification path. Coordinates: AEC Mandatory Disclosure (2022):
-- https://www.aecwb.edu.in/disclosure/ManDis_AEC2022.pdf
UPDATE projects
SET latitude = 23.7156390,
    longitude = 86.9514520
WHERE project_id IN (
    'DEMO-MPL-1001', 'DEMO-MPL-1002', 'DEMO-MPL-1003', 'DEMO-MPL-1004',
    'DEMO-MPL-1005', 'DEMO-MPL-1006', 'DEMO-MPL-1007', 'DEMO-MPL-1008',
    'DEMO-MPL-1009', 'DEMO-MPL-1010'
);

ALTER TABLE projects MODIFY COLUMN contractor_id VARCHAR(50) NOT NULL;

CREATE TABLE IF NOT EXISTS progress_evidence (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT UNSIGNED NOT NULL,
    contractor_id VARCHAR(50) NOT NULL,
    photo_reference VARCHAR(255) NULL,
    submitted_latitude DECIMAL(10,7) NULL,
    submitted_longitude DECIMAL(10,7) NULL,
    distance_metres DECIMAL(12,2) NULL,
    verification_status VARCHAR(60) NOT NULL,
    submitted_at DATETIME NOT NULL,
    completion_percentage DECIMAL(5,2) NULL,
    expenditure DECIMAL(15,2) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_evidence_project(project_id),
    CONSTRAINT fk_evidence_project FOREIGN KEY(project_id) REFERENCES projects(id)
);
