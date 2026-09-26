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
    INDEX idx_monthly_closing_execution_version
        (tenant_id, monthly_closing_id, closing_version),
    INDEX idx_monthly_closing_execution_status
        (tenant_id, status, started_at),
    CONSTRAINT fk_monthly_closing_execution_closing
        FOREIGN KEY (monthly_closing_id)
        REFERENCES monthly_closings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 失敗履歴を残したまま同じ締めVersionを再実行できるようにする。
-- Versionは成功した締めだけが消費し、実行履歴は試行単位で保持する。
SET @drop_execution_version_unique = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE monthly_closing_execution DROP INDEX uk_monthly_closing_execution_version',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_closing_execution'
      AND index_name = 'uk_monthly_closing_execution_version'
);
PREPARE drop_execution_version_unique_statement
    FROM @drop_execution_version_unique;
EXECUTE drop_execution_version_unique_statement;
DEALLOCATE PREPARE drop_execution_version_unique_statement;

SET @add_execution_version_index = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE monthly_closing_execution ADD INDEX idx_monthly_closing_execution_version (tenant_id, monthly_closing_id, closing_version)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_closing_execution'
      AND index_name = 'idx_monthly_closing_execution_version'
);
PREPARE add_execution_version_index_statement
    FROM @add_execution_version_index;
EXECUTE add_execution_version_index_statement;
DEALLOCATE PREPARE add_execution_version_index_statement;

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

-- Spreadsheet台帳は最新データを随時生成するため、月次締めの実行対象には含めない。
-- 履歴互換のため定義は残し、required_flag/active_flagをFALSEに固定する。
-- output_codeはexcel_book_master.book_codeと一致させる。
INSERT INTO monthly_closing_output_definition (
    output_type,
    output_code,
    execution_order,
    required_flag,
    active_flag,
    backup_retention_years,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
) VALUES
    (
        'LEDGER', 'MONTHLY_LABOR', 30,
        FALSE, FALSE, 7, 'default',
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
    ),
    (
        'LEDGER', 'LABOR_COST_PAYMENT', 40,
        FALSE, FALSE, 7, 'default',
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
    ),
    (
        'LEDGER', 'RECEIPT_CONFIRMATION', 50,
        FALSE, FALSE, 7, 'default',
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
    ),
    (
        'LEDGER', 'MONTHLY_SUMMARY', 60,
        FALSE, FALSE, 7, 'default',
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
    )
ON DUPLICATE KEY UPDATE
    execution_order = VALUES(execution_order),
    required_flag = VALUES(required_flag),
    active_flag = VALUES(active_flag),
    backup_retention_years = VALUES(backup_retention_years),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);
