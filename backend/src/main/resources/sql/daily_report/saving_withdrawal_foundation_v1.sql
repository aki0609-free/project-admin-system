-- ProjectAdmin V1 日報貯金引出基盤
-- 貯金引出は日報だけから入力し、給与課税額には含めず差引支給額へ加算する。

SET @saving_withdrawal_amount_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'saving_withdrawal_amount'
);
SET @saving_withdrawal_amount_sql := IF(
    @saving_withdrawal_amount_exists = 0,
    'ALTER TABLE daily_report ADD COLUMN saving_withdrawal_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER saving_amount',
    'SELECT 1'
);
PREPARE statement FROM @saving_withdrawal_amount_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- Hibernate等が先に列を作成した環境でも、既存INSERTとの互換性を保つ。
ALTER TABLE daily_report
    MODIFY COLUMN saving_withdrawal_amount DECIMAL(12,2) NOT NULL DEFAULT 0;
