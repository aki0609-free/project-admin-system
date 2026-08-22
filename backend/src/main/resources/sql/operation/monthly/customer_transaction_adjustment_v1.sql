-- ProjectAdminSystem V1
-- 入金差額を業務上の理由付きで調整するための符号付き金額。

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE customer_transactions ADD COLUMN adjustment_amount INT NOT NULL DEFAULT 0 AFTER offset_amount',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customer_transactions'
      AND column_name = 'adjustment_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE customer_transactions
SET adjustment_amount = 0
WHERE adjustment_amount IS NULL;
