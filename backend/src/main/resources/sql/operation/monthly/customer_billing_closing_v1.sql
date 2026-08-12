-- 顧客別締日に基づく請求締め。給与・社内月次締めとは独立して管理する。
CREATE TABLE IF NOT EXISTS customer_billing_closings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_month DATE NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    closing_version INT NOT NULL DEFAULT 0,
    closed_at TIMESTAMP(6) NULL,
    closed_by VARCHAR(100) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_billing_closing_customer
        UNIQUE (tenant_id, target_month, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2026-08-11より前の対象月単位テーブルを、対象月＋顧客単位へ拡張する。
-- 旧行はcustomer_id=NULLの監査データとして残し、新処理の検索対象には含めない。
SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE customer_billing_closings ADD COLUMN customer_id BIGINT NULL AFTER target_month',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_billing_closings'
      AND column_name = 'customer_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE customer_billing_closings DROP INDEX uk_customer_billing_closing_month',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_billing_closings'
      AND index_name = 'uk_customer_billing_closing_month'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE customer_billing_closings ADD CONSTRAINT uk_customer_billing_closing_customer UNIQUE (tenant_id, target_month, customer_id)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_billing_closings'
      AND index_name = 'uk_customer_billing_closing_customer'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE monthly_closing_report_files ADD COLUMN closing_scope VARCHAR(30) NOT NULL DEFAULT ''COMPANY'' AFTER monthly_closing_id',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_closing_report_files'
      AND column_name = 'closing_scope'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
