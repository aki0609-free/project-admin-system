-- ProjectAdmin V1 手当・控除の数量残高・繰越共通基盤
-- 項目ごとの物理テーブルは作らず、ポリシーと従業員別対象期間で管理する。

CREATE TABLE IF NOT EXISTS payroll_item_balance_policy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_type VARCHAR(20) NOT NULL,
    target_master_id BIGINT NOT NULL,
    target_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    balance_unit VARCHAR(20) NOT NULL,
    accrual_frequency VARCHAR(20) NOT NULL,
    accrual_rule_name VARCHAR(100) NOT NULL,
    carry_forward_flag BOOLEAN NOT NULL DEFAULT TRUE,
    advance_consumption_flag BOOLEAN NOT NULL DEFAULT FALSE,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payroll_item_balance_policy_code
        UNIQUE (tenant_id, target_type, target_code),
    CONSTRAINT chk_payroll_item_balance_policy_target
        CHECK (target_type IN ('ALLOWANCE', 'DEDUCTION')),
    CONSTRAINT chk_payroll_item_balance_policy_unit
        CHECK (balance_unit IN ('DAYS', 'HOURS', 'COUNT', 'AMOUNT')),
    CONSTRAINT chk_payroll_item_balance_policy_frequency
        CHECK (accrual_frequency IN ('MONTHLY', 'DAILY', 'MANUAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @balance_policy_display_name_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'payroll_item_balance_policy'
      AND column_name = 'display_name'
);
SET @balance_policy_display_name_sql := IF(
    @balance_policy_display_name_exists = 0,
    'ALTER TABLE payroll_item_balance_policy ADD COLUMN display_name VARCHAR(200) NOT NULL DEFAULT '''' AFTER target_code',
    'SELECT 1'
);
PREPARE statement FROM @balance_policy_display_name_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS employee_payroll_item_enrollment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    balance_policy_id BIGINT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    settings_json JSON NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_employee_balance_enrollment_period
        (tenant_id, employee_id, balance_policy_id, effective_from, effective_to),
    CONSTRAINT fk_employee_balance_enrollment_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT fk_employee_balance_enrollment_policy
        FOREIGN KEY (balance_policy_id) REFERENCES payroll_item_balance_policy (id),
    CONSTRAINT chk_employee_balance_enrollment_period
        CHECK (effective_to IS NULL OR effective_to >= effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @enrollment_settings_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'employee_payroll_item_enrollment'
      AND column_name = 'settings_json'
);
SET @enrollment_settings_sql := IF(
    @enrollment_settings_exists = 0,
    'ALTER TABLE employee_payroll_item_enrollment ADD COLUMN settings_json JSON NULL AFTER effective_to',
    'SELECT 1'
);
PREPARE statement FROM @enrollment_settings_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @deduction_calculated_amount_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_deductions'
      AND column_name = 'calculated_amount'
);
SET @deduction_calculated_amount_sql := IF(
    @deduction_calculated_amount_exists = 0,
    'ALTER TABLE daily_report_deductions ADD COLUMN calculated_amount INT NOT NULL DEFAULT 0 AFTER amount',
    'SELECT 1'
);
PREPARE statement FROM @deduction_calculated_amount_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @deduction_manual_override_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_deductions'
      AND column_name = 'manual_override_flag'
);
SET @deduction_manual_override_sql := IF(
    @deduction_manual_override_exists = 0,
    'ALTER TABLE daily_report_deductions ADD COLUMN manual_override_flag BOOLEAN NOT NULL DEFAULT FALSE AFTER calculated_amount',
    'SELECT 1'
);
PREPARE statement FROM @deduction_manual_override_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @deduction_override_reason_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_deductions'
      AND column_name = 'override_reason'
);
SET @deduction_override_reason_sql := IF(
    @deduction_override_reason_exists = 0,
    'ALTER TABLE daily_report_deductions ADD COLUMN override_reason VARCHAR(500) NULL AFTER manual_override_flag',
    'SELECT 1'
);
PREPARE statement FROM @deduction_override_reason_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @deduction_quantity_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_deductions'
      AND column_name = 'quantity'
);
SET @deduction_quantity_sql := IF(
    @deduction_quantity_exists = 0,
    'ALTER TABLE daily_report_deductions ADD COLUMN quantity DECIMAL(12,2) NULL AFTER override_reason',
    'SELECT 1'
);
PREPARE statement FROM @deduction_quantity_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @deduction_balance_unit_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_deductions'
      AND column_name = 'balance_unit'
);
SET @deduction_balance_unit_sql := IF(
    @deduction_balance_unit_exists = 0,
    'ALTER TABLE daily_report_deductions ADD COLUMN balance_unit VARCHAR(20) NULL AFTER quantity',
    'SELECT 1'
);
PREPARE statement FROM @deduction_balance_unit_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- 移行前の明細にはRule基準額がないため、当時の確定金額を基準額として引き継ぐ。
SET @deduction_calculated_amount_migration_sql := IF(
    @deduction_calculated_amount_exists = 0,
    'UPDATE daily_report_deductions SET calculated_amount = amount',
    'SELECT 1'
);
PREPARE statement FROM @deduction_calculated_amount_migration_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- 過去の初期データでJava Enumに存在しない値がMySQL ENUMの空文字へ
-- 変換されている場合、詳細画面を持たない正規値へ統一する。
UPDATE deduction_masters
SET detail_view_type = 'NONE',
    updated_at = NOW(6)
WHERE detail_view_type = '';

-- 既存寮費明細へ、日報に保存済みの徴収日数を移行する。
UPDATE daily_report_deductions deduction_item
JOIN daily_report report ON report.id = deduction_item.daily_report_id
SET deduction_item.calculated_amount = deduction_item.amount,
    deduction_item.quantity = report.dormitory_charge_days,
    deduction_item.balance_unit = 'DAYS',
    deduction_item.updated_at = NOW(6)
WHERE deduction_item.deduction_code = 'DORMITORY_FEE'
  AND deduction_item.quantity IS NULL;

-- 寮費をRule基準値＋理由付き上書き可能にする。
INSERT INTO deduction_masters (
    deduction_code, deduction_name, deduction_type, calculation_type,
    rule_name, default_amount, allow_manual_input,
    deduction_unit, detail_view_type,
    show_on_daily_statement, show_on_monthly_statement,
    carry_to_monthly_settlement, display_order, enabled, note,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT 'DORMITORY_FEE', '寮費', 'COMPANY', 'AUTO',
       'DORMITORY_DAILY_FEE', 0, TRUE,
       'BOTH', 'NONE', TRUE, TRUE, TRUE, 110, TRUE,
       '日次徴収・残日数繰越対象', 'default', NOW(6), NOW(6), NULL
WHERE NOT EXISTS (
    SELECT 1 FROM deduction_masters
    WHERE tenant_id = 'default' AND deduction_code = 'DORMITORY_FEE'
      AND deleted_at IS NULL
);

UPDATE deduction_masters
SET calculation_type = 'AUTO',
    rule_name = 'DORMITORY_DAILY_FEE',
    allow_manual_input = TRUE,
    updated_at = NOW(6)
WHERE deduction_code = 'DORMITORY_FEE'
  AND deleted_at IS NULL;

-- 旧SQLでJava Enumに存在しない値がMySQL ENUMの空文字として保存された場合を補正する。
UPDATE deduction_masters
SET detail_view_type = 'NONE',
    updated_at = NOW(6)
WHERE deduction_code = 'DORMITORY_FEE'
  AND (detail_view_type = '' OR detail_view_type IS NULL)
  AND deleted_at IS NULL;

INSERT INTO payroll_item_balance_policy (
    target_type, target_master_id, target_code, display_name,
    balance_unit, accrual_frequency, accrual_rule_name,
    carry_forward_flag, advance_consumption_flag, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT 'DEDUCTION', deduction.id, deduction.deduction_code, deduction.deduction_name,
       'DAYS', 'MONTHLY', 'CALENDAR_DAYS_IN_ENROLLMENT',
       TRUE, FALSE, TRUE,
       deduction.tenant_id, NOW(6), NOW(6), NULL
FROM deduction_masters deduction
WHERE deduction.deduction_code = 'DORMITORY_FEE'
  AND deduction.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    target_master_id = VALUES(target_master_id),
    display_name = VALUES(display_name),
    balance_unit = VALUES(balance_unit),
    accrual_frequency = VALUES(accrual_frequency),
    accrual_rule_name = VALUES(accrual_rule_name),
    updated_at = NOW(6),
    deleted_at = NULL;

-- 既に入寮中の従業員は、基盤適用日から自動的に対象とする。
INSERT INTO employee_payroll_item_enrollment (
    employee_id, balance_policy_id, effective_from, effective_to, settings_json,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT employee.id, policy.id,
       CAST(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01') AS DATE), NULL,
       JSON_OBJECT('dormitoryType', employee.dormitory_type),
       employee.tenant_id, NOW(6), NOW(6), NULL
FROM employee
JOIN payroll_item_balance_policy policy
  ON policy.tenant_id = employee.tenant_id
 AND policy.target_type = 'DEDUCTION'
 AND policy.target_code = 'DORMITORY_FEE'
 AND policy.deleted_at IS NULL
WHERE employee.dormitory_flag = TRUE
  AND employee.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM employee_payroll_item_enrollment enrollment
      WHERE enrollment.employee_id = employee.id
        AND enrollment.balance_policy_id = policy.id
        AND enrollment.effective_to IS NULL
        AND enrollment.deleted_at IS NULL
  );

-- 携帯電話貸出料も同じ残高基盤へ登録できる。従業員への適用期間は別途有効化する。
INSERT INTO deduction_masters (
    deduction_code, deduction_name, deduction_type, calculation_type,
    default_amount, allow_manual_input,
    deduction_unit, detail_view_type,
    show_on_daily_statement, show_on_monthly_statement,
    carry_to_monthly_settlement, display_order, enabled, note,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT 'MOBILE_RENTAL', '携帯電話貸出料', 'COMPANY', 'FIXED',
       0, TRUE, 'BOTH', 'NONE', TRUE, TRUE, TRUE, 120, TRUE,
       '日次徴収・残日数繰越対象', 'default', NOW(6), NOW(6), NULL
WHERE NOT EXISTS (
    SELECT 1 FROM deduction_masters
    WHERE tenant_id = 'default' AND deduction_code = 'MOBILE_RENTAL'
      AND deleted_at IS NULL
);

UPDATE deduction_masters
SET deduction_unit = 'BOTH',
    show_on_daily_statement = TRUE,
    allow_manual_input = TRUE,
    updated_at = NOW(6)
WHERE deduction_code = 'MOBILE_RENTAL'
  AND deleted_at IS NULL;

UPDATE deduction_masters
SET detail_view_type = 'NONE',
    updated_at = NOW(6)
WHERE deduction_code = 'MOBILE_RENTAL'
  AND (detail_view_type = '' OR detail_view_type IS NULL)
  AND deleted_at IS NULL;

INSERT INTO payroll_item_balance_policy (
    target_type, target_master_id, target_code, display_name,
    balance_unit, accrual_frequency, accrual_rule_name,
    carry_forward_flag, advance_consumption_flag, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT 'DEDUCTION', deduction.id, deduction.deduction_code, deduction.deduction_name,
       'DAYS', 'MONTHLY', 'CALENDAR_DAYS_IN_ENROLLMENT',
       TRUE, FALSE, TRUE,
       deduction.tenant_id, NOW(6), NOW(6), NULL
FROM deduction_masters deduction
WHERE deduction.deduction_code = 'MOBILE_RENTAL'
  AND deduction.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    target_master_id = VALUES(target_master_id),
    display_name = VALUES(display_name),
    active_flag = TRUE,
    updated_at = NOW(6);
