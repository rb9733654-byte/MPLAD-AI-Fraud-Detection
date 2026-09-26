-- Add persistent usernames to role-specific account tables. Safe to rerun.
USE mplad_db;

SET @mplad_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'workspace_users' AND column_name = 'username'
);
SET @mplad_ddl = IF(@mplad_column_exists = 0,
    'ALTER TABLE workspace_users ADD COLUMN username VARCHAR(50) NULL AFTER workspace_role', 'SELECT 1');
PREPARE mplad_username_stmt FROM @mplad_ddl;
EXECUTE mplad_username_stmt;
DEALLOCATE PREPARE mplad_username_stmt;

UPDATE workspace_users
SET username = CONCAT('member_', id)
WHERE username IS NULL OR TRIM(username) = '';

ALTER TABLE workspace_users MODIFY COLUMN username VARCHAR(50) NOT NULL;

SET @mplad_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'workspace_users' AND index_name = 'uq_workspace_role_username'
);
SET @mplad_ddl = IF(@mplad_index_exists = 0,
    'CREATE UNIQUE INDEX uq_workspace_role_username ON workspace_users (workspace_role, username)', 'SELECT 1');
PREPARE mplad_username_stmt FROM @mplad_ddl;
EXECUTE mplad_username_stmt;
DEALLOCATE PREPARE mplad_username_stmt;

SET @mplad_column_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'public_users' AND column_name = 'username'
);
SET @mplad_ddl = IF(@mplad_column_exists = 0,
    'ALTER TABLE public_users ADD COLUMN username VARCHAR(50) NULL AFTER id', 'SELECT 1');
PREPARE mplad_username_stmt FROM @mplad_ddl;
EXECUTE mplad_username_stmt;
DEALLOCATE PREPARE mplad_username_stmt;

UPDATE public_users
SET username = CONCAT('member_', id)
WHERE username IS NULL OR TRIM(username) = '';

ALTER TABLE public_users MODIFY COLUMN username VARCHAR(50) NOT NULL;

SET @mplad_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'public_users' AND index_name = 'uq_public_users_username'
);
SET @mplad_ddl = IF(@mplad_index_exists = 0,
    'CREATE UNIQUE INDEX uq_public_users_username ON public_users (username)', 'SELECT 1');
PREPARE mplad_username_stmt FROM @mplad_ddl;
EXECUTE mplad_username_stmt;
DEALLOCATE PREPARE mplad_username_stmt;
