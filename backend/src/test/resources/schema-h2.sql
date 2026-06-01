CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP NULL,
    UNIQUE KEY uk_user_account_email (email)
);

CREATE TABLE IF NOT EXISTS data_source_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    host VARCHAR(255) NULL,
    port INT NULL,
    database_name VARCHAR(128) NULL,
    username VARCHAR(128) NULL,
    secret_value VARCHAR(1024) NULL,
    api_base_url VARCHAR(512) NULL,
    notes VARCHAR(1024) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_test_status VARCHAR(32) NULL,
    last_test_message VARCHAR(512) NULL,
    last_tested_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_data_source_config_name (name)
);

CREATE TABLE IF NOT EXISTS admin_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    progress INT NOT NULL,
    message VARCHAR(512) NULL,
    details_json TEXT NULL,
    created_by VARCHAR(128) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS system_setting (
    setting_key VARCHAR(64) PRIMARY KEY,
    setting_value VARCHAR(1024) NOT NULL,
    category_name VARCHAR(32) NOT NULL,
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
