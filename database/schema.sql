-- MPLAD AI Fraud Detection: MySQL 8.0 starter schema.
-- All sample values below are fictional demo data for development and testing.

CREATE DATABASE IF NOT EXISTS mplad_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mplad_db;

CREATE TABLE IF NOT EXISTS projects (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    -- Stable external identifier used by frontend, API, and AI data exchanges.
    project_id VARCHAR(50) NOT NULL,
    constituency VARCHAR(120) NOT NULL,
    district VARCHAR(120) NOT NULL,
    project_type VARCHAR(80) NOT NULL,
    -- Financial values are stored in lakh rupees for the demo dataset.
    sanctioned_amount DECIMAL(15, 2) UNSIGNED NOT NULL,
    released_amount DECIMAL(15, 2) UNSIGNED NOT NULL,
    actual_expenditure DECIMAL(15, 2) UNSIGNED NOT NULL,
    completion_percentage DECIMAL(5, 2) UNSIGNED NOT NULL,
    project_duration_days INT UNSIGNED NOT NULL,
    delay_days INT UNSIGNED NOT NULL DEFAULT 0,
    contractor_previous_projects INT UNSIGNED NOT NULL DEFAULT 0,
    contractor_avg_cost DECIMAL(15, 2) UNSIGNED NOT NULL,
    expenditure_per_completion_percent DECIMAL(18, 4) UNSIGNED NOT NULL,
    projects_by_contractor INT UNSIGNED NOT NULL DEFAULT 0,
    is_completed TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_projects_project_id (project_id),
    KEY idx_projects_district (district),
    KEY idx_projects_constituency (constituency),
    KEY idx_projects_project_type (project_type),
    CONSTRAINT chk_projects_completion_percentage
        CHECK (completion_percentage <= 100.00),
    CONSTRAINT chk_projects_is_completed
        CHECK (is_completed IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ten entirely fictional demo records. They intentionally contain varied project
-- progress, delay, and expenditure patterns for future API/UI/AI testing only.
INSERT INTO projects (
    project_id, constituency, district, project_type,
    sanctioned_amount, released_amount, actual_expenditure,
    completion_percentage, project_duration_days, delay_days,
    contractor_previous_projects, contractor_avg_cost,
    expenditure_per_completion_percent, projects_by_contractor, is_completed
) VALUES
    ('DEMO-MPL-1001', 'Demo Constituency Aurora', 'Sample District North', 'Road Improvement',
     48.00, 42.00, 39.80, 92.00, 365, 0, 6, 46.50, 0.4326, 3, 0),
    ('DEMO-MPL-1002', 'Demo Constituency Aurora', 'Sample District North', 'Community Health Centre',
     82.00, 75.00, 75.00, 100.00, 420, 12, 9, 79.20, 0.7500, 4, 1),
    ('DEMO-MPL-1003', 'Demo Constituency Birch', 'Sample District East', 'School Renovation',
     55.00, 50.00, 31.50, 58.00, 300, 84, 4, 52.30, 0.5431, 2, 0),
    ('DEMO-MPL-1004', 'Demo Constituency Cedar', 'Sample District West', 'Water Supply',
     110.00, 96.00, 91.20, 81.00, 540, 36, 12, 104.00, 1.1259, 5, 0),
    ('DEMO-MPL-1005', 'Demo Constituency Birch', 'Sample District East', 'Solar Lighting',
     36.00, 32.00, 29.40, 100.00, 240, 0, 3, 34.10, 0.2940, 2, 1),
    ('DEMO-MPL-1006', 'Demo Constituency Delta', 'Sample District South', 'Public Library',
     44.00, 20.00, 8.50, 22.00, 270, 138, 5, 42.70, 0.3864, 3, 0),
    ('DEMO-MPL-1007', 'Demo Constituency Echo', 'Sample District Central', 'Sports Ground',
     29.00, 27.00, 26.20, 96.00, 210, 4, 7, 28.40, 0.2729, 4, 0),
    -- Fictional high-expenditure pattern for anomaly-review demonstrations.
    ('DEMO-MPL-1008', 'Demo Constituency Delta', 'Sample District South', 'Drainage Improvement',
     68.00, 61.00, 85.50, 43.00, 390, 205, 8, 70.10, 1.9884, 31, 0),
    -- Fictional low-progress and long-delay pattern; not a fraud determination.
    ('DEMO-MPL-1009', 'Demo Constituency Echo', 'Sample District Central', 'Anganwadi Upgrade',
     41.00, 38.00, 33.60, 39.00, 300, 276, 15, 41.80, 0.8615, 18, 0),
    ('DEMO-MPL-1010', 'Demo Constituency Cedar', 'Sample District West', 'Digital Learning Centre',
     63.00, 59.00, 58.40, 100.00, 330, 8, 10, 60.20, 0.5840, 5, 1);
