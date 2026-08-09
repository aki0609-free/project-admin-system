-- 年度帳票バックアップ実行履歴・ファイル台帳
-- 生成済み帳票を documents/backups/reports へコピーし、再実行を冪等にする。

CREATE TABLE IF NOT EXISTS annual_report_backup_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_code VARCHAR(50) NOT NULL,
    fiscal_year_start_month INT NOT NULL DEFAULT 4,
    grace_days INT NOT NULL DEFAULT 14,
    startup_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_annual_report_backup_setting_code
        UNIQUE (tenant_id, setting_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS annual_report_backup_execution (
    id BIGINT NOT NULL AUTO_INCREMENT,
    fiscal_year INT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    eligible_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    file_count INT NOT NULL DEFAULT 0,
    total_size BIGINT NOT NULL DEFAULT 0,
    error_message LONGTEXT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_annual_report_backup_fiscal_year
        UNIQUE (tenant_id, fiscal_year),
    INDEX idx_annual_report_backup_status
        (tenant_id, status, eligible_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS annual_report_backup_file (
    id BIGINT NOT NULL AUTO_INCREMENT,
    backup_execution_id BIGINT NOT NULL,
    monthly_closing_report_file_id BIGINT NOT NULL,
    report_code VARCHAR(100) NOT NULL,
    target_month VARCHAR(7) NOT NULL,
    closing_version INT NOT NULL,
    storage_type VARCHAR(30) NOT NULL,
    source_file_key VARCHAR(1000) NOT NULL,
    backup_file_key VARCHAR(1000) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    copied_at TIMESTAMP(6) NOT NULL,
    retention_until DATE NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_annual_report_backup_source_file
        UNIQUE (tenant_id, monthly_closing_report_file_id),
    INDEX idx_annual_report_backup_file_execution
        (tenant_id, backup_execution_id),
    INDEX idx_annual_report_backup_file_retention
        (tenant_id, retention_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
