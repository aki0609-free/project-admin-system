-- ProjectAdmin 月次締め・帳票・台帳基盤 V1
-- MySQL 8.x
-- 既存データを削除しない追加DDL。

CREATE TABLE IF NOT EXISTS monthly_closing_execution (
    id BIGINT NOT NULL AUTO_INCREMENT,
    monthly_closing_id BIGINT NOT NULL,
    closing_version INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP(6) NOT NULL,
    completed_at TIMESTAMP(6) NULL,
    executed_by VARCHAR(100) NOT NULL,
    error_message LONGTEXT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_monthly_closing_execution_version
        UNIQUE (tenant_id, monthly_closing_id, closing_version),
    INDEX idx_monthly_closing_execution_status
        (tenant_id, status, started_at),
    CONSTRAINT fk_monthly_closing_execution_closing
        FOREIGN KEY (monthly_closing_id)
        REFERENCES monthly_closings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_closing_output_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    output_type VARCHAR(30) NOT NULL,
    output_code VARCHAR(100) NOT NULL,
    execution_order INT NOT NULL DEFAULT 1,
    required_flag BOOLEAN NOT NULL DEFAULT TRUE,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    backup_retention_years INT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_monthly_closing_output_definition
        UNIQUE (tenant_id, output_type, output_code),
    INDEX idx_monthly_closing_output_active
        (tenant_id, active_flag, execution_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_closing_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    monthly_closing_execution_id BIGINT NOT NULL,
    output_type VARCHAR(30) NOT NULL,
    output_code VARCHAR(100) NOT NULL,
    target_key VARCHAR(255) NOT NULL DEFAULT 'ALL',
    required_flag BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(30) NOT NULL,
    history_row_count BIGINT NULL,
    history_table VARCHAR(200) NULL,
    storage_type VARCHAR(30) NULL,
    file_key VARCHAR(1000) NULL,
    file_name VARCHAR(500) NULL,
    content_type VARCHAR(100) NULL,
    file_size BIGINT NULL,
    file_hash VARCHAR(128) NULL,
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    error_message LONGTEXT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_monthly_closing_item_target
        UNIQUE (
            tenant_id,
            monthly_closing_execution_id,
            output_type,
            output_code,
            target_key
        ),
    INDEX idx_monthly_closing_item_status
        (tenant_id, monthly_closing_execution_id, status),
    CONSTRAINT fk_monthly_closing_item_execution
        FOREIGN KEY (monthly_closing_execution_id)
        REFERENCES monthly_closing_execution (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初期マスター例。実際の帳票・台帳コード確定後に有効化する。
-- INSERT INTO monthly_closing_output_definition (
--     output_type, output_code, execution_order,
--     required_flag, active_flag, backup_retention_years,
--     tenant_id, created_at, updated_at
-- ) VALUES
--     ('REPORT', 'MONTHLY_PAY_SLIP', 10, TRUE, TRUE, 7,
--      'default', NOW(6), NOW(6)),
--     ('REPORT', 'MONTHLY_INVOICE', 20, TRUE, TRUE, 7,
--      'default', NOW(6), NOW(6)),
--     ('LEDGER', 'MONTHLY_LABOR_TABLE', 30, TRUE, TRUE, NULL,
--      'default', NOW(6), NOW(6)),
--     ('LEDGER', 'MONTHLY_LABOR_COST', 40, TRUE, TRUE, NULL,
--      'default', NOW(6), NOW(6)),
--     ('LEDGER', 'PAYMENT_CONFIRMATION', 50, TRUE, TRUE, NULL,
--      'default', NOW(6), NOW(6)),
--     ('LEDGER', 'MONTHLY_SUMMARY', 60, TRUE, TRUE, NULL,
--      'default', NOW(6), NOW(6));
