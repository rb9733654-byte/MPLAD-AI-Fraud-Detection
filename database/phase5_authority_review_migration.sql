-- Idempotent Phase 5 authority review and contractor notification tables.
-- CREATE TABLE IF NOT EXISTS preserves existing rows and is a no-op when a table exists.
USE mplad_db;

CREATE TABLE IF NOT EXISTS authority_reviews (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT UNSIGNED NOT NULL,
    review_status VARCHAR(80) NOT NULL,
    remark VARCHAR(1000) NOT NULL,
    required_action VARCHAR(300) NULL,
    review_date DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_reviews_project_date(project_id, review_date),
    CONSTRAINT fk_reviews_project FOREIGN KEY(project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS contractor_notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    contractor_id VARCHAR(50) NOT NULL,
    project_id BIGINT UNSIGNED NOT NULL,
    review_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(160) NOT NULL,
    message VARCHAR(1500) NOT NULL,
    notification_type VARCHAR(60) NOT NULL,
    is_read TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    KEY idx_notifications_contractor(contractor_id, created_at),
    CONSTRAINT fk_notifications_project FOREIGN KEY(project_id) REFERENCES projects(id),
    CONSTRAINT fk_notifications_review FOREIGN KEY(review_id) REFERENCES authority_reviews(id)
);
