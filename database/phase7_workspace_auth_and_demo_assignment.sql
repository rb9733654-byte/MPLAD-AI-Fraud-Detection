-- Demo workspace identities and one-time contractor assignment normalization.
-- Safe to rerun. This script never drops data or runs automatically on backend startup.
USE mplad_db;

CREATE TABLE IF NOT EXISTS workspace_users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    workspace_role VARCHAR(20) NOT NULL,
    identity_value VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_workspace_role_identity (workspace_role, identity_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Intentional demo migration: all ten fictional projects belong to the same
-- demo contractor so the contractor workflow can demonstrate the full register.
UPDATE projects
SET contractor_id = 'DEMO-CONTRACTOR-01'
WHERE project_id IN (
    'DEMO-MPL-1001', 'DEMO-MPL-1002', 'DEMO-MPL-1003', 'DEMO-MPL-1004', 'DEMO-MPL-1005',
    'DEMO-MPL-1006', 'DEMO-MPL-1007', 'DEMO-MPL-1008', 'DEMO-MPL-1009', 'DEMO-MPL-1010'
);

-- Keep existing review notifications attached to the contractor now assigned
-- to their demo project; no notification rows are removed.
UPDATE contractor_notifications n
JOIN projects p ON p.id = n.project_id
SET n.contractor_id = p.contractor_id
WHERE p.project_id IN (
    'DEMO-MPL-1001', 'DEMO-MPL-1002', 'DEMO-MPL-1003', 'DEMO-MPL-1004', 'DEMO-MPL-1005',
    'DEMO-MPL-1006', 'DEMO-MPL-1007', 'DEMO-MPL-1008', 'DEMO-MPL-1009', 'DEMO-MPL-1010'
);
