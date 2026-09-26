-- Public prototype authentication and feedback tables. Safe to rerun; preserves existing data.
USE mplad_db;

CREATE TABLE IF NOT EXISTS public_users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    email_verified TINYINT(1) NOT NULL DEFAULT 0,
    account_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_public_users_email (email),
    KEY idx_public_users_status (account_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS public_otp_verifications (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_user_id BIGINT UNSIGNED NOT NULL,
    email VARCHAR(254) NOT NULL,
    otp_hash VARCHAR(100) NOT NULL,
    expires_at DATETIME NOT NULL,
    attempts INT UNSIGNED NOT NULL DEFAULT 0,
    verified TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_public_otp_user_latest (public_user_id, id),
    KEY idx_public_otp_expiry (expires_at),
    CONSTRAINT fk_public_otp_user FOREIGN KEY (public_user_id) REFERENCES public_users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS public_feedback (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    project_id BIGINT UNSIGNED NOT NULL,
    public_user_id BIGINT UNSIGNED NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    comment VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_public_feedback_project_created (project_id, id),
    KEY idx_public_feedback_user (public_user_id),
    CONSTRAINT fk_public_feedback_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_public_feedback_user FOREIGN KEY (public_user_id) REFERENCES public_users(id),
    CONSTRAINT chk_public_feedback_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
