-- ProjectAdminSystem V1
-- 日報給与内訳Rule・保存項目の基盤

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN normal_pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER saving_amount',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'normal_pay_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN overtime_pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER normal_pay_amount',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'overtime_pay_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN night_pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER overtime_pay_amount',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'night_pay_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN holiday_pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER night_pay_amount',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'holiday_pay_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS daily_pay_rule_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    component_type VARCHAR(30) NOT NULL,
    rule_name VARCHAR(150) NOT NULL,
    active_flag TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_daily_pay_rule_setting_tenant_component
        UNIQUE (tenant_id, component_type)
);

-- 寮費は会社への支払いは月次だが、従業員からは日次徴収する。
-- 日報控除明細へ日ごとの確定額を保存し、月次はその合計を使用する。
UPDATE deduction_masters
SET deduction_unit = 'BOTH',
    show_on_daily_statement = 1,
    show_on_monthly_statement = 1,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE deduction_code = 'DORMITORY_FEE'
  AND deleted_at IS NULL;

-- Rule本体をRule管理画面で登録した後、対応だけを設定する。
-- INSERT INTO daily_pay_rule_setting (
--     component_type, rule_name, active_flag,
--     created_at, updated_at, tenant_id
-- ) VALUES
--     ('NORMAL_PAY',   'DAILY_NORMAL_PAY',   1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
--     ('OVERTIME_PAY', 'DAILY_OVERTIME_PAY', 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
--     ('NIGHT_PAY',    'DAILY_NIGHT_PAY',    1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
--     ('HOLIDAY_PAY',  'DAILY_HOLIDAY_PAY',  1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default');
