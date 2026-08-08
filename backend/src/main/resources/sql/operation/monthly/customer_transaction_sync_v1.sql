-- ProjectAdmin V1
-- 月次締め確定請求履歴と顧客取引の追跡・重複防止

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE customer_transactions ADD COLUMN source_type VARCHAR(30) NULL AFTER note',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_transactions'
      AND column_name = 'source_type'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE customer_transactions ADD COLUMN source_invoice_history_id BIGINT NULL AFTER source_type',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_transactions'
      AND column_name = 'source_invoice_history_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE customer_transactions ADD COLUMN source_closing_version INT NULL AFTER source_invoice_history_id',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_transactions'
      AND column_name = 'source_closing_version'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 既存重複がある場合は意図せず統合せず、ALTERを失敗させて手動確認する。
SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE customer_transactions ADD CONSTRAINT uk_customer_transaction_month UNIQUE (tenant_id, customer_id, target_month)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_transactions'
      AND index_name = 'uk_customer_transaction_month'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
