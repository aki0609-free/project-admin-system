-- ProjectAdmin V1 手当・控除の数量残高・繰越共通基盤
-- 項目ごとの物理テーブルは作らず、ポリシーと従業員別対象期間で管理する。

CREATE TABLE IF NOT EXISTS payroll_item_balance_policy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_type VARCHAR(20) NOT NULL,
    target_master_id BIGINT NOT NULL,
    target_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    application_scope VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE_ENROLLMENT',
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

-- Hibernateが新Entity定義から先にテーブルを作る場合と、既存DBへ段階適用する場合の
-- どちらでも後続INSERTが同じ列構成を利用できるよう、入力方式をここで先に揃える。
SET @balance_tracking_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payroll_item_balance_policy'
      AND column_name = 'balance_tracking_flag'
);
SET @balance_tracking_sql := IF(
    @balance_tracking_exists = 0,
    'ALTER TABLE payroll_item_balance_policy ADD COLUMN balance_tracking_flag BOOLEAN NOT NULL DEFAULT TRUE AFTER balance_unit',
    'SELECT 1'
);
PREPARE statement FROM @balance_tracking_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @application_scope_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payroll_item_balance_policy'
      AND column_name = 'application_scope'
);
SET @application_scope_sql := IF(
    @application_scope_exists = 0,
    'ALTER TABLE payroll_item_balance_policy ADD COLUMN application_scope VARCHAR(30) NOT NULL DEFAULT ''EMPLOYEE_ENROLLMENT'' AFTER display_name',
    'SELECT 1'
);
PREPARE statement FROM @application_scope_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @input_source_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payroll_item_balance_policy'
      AND column_name = 'input_source'
);
SET @input_source_sql := IF(
    @input_source_exists = 0,
    'ALTER TABLE payroll_item_balance_policy ADD COLUMN input_source VARCHAR(30) NOT NULL DEFAULT ''DAILY_REPORT'' AFTER balance_tracking_flag',
    'SELECT 1'
);
PREPARE statement FROM @input_source_sql;
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

CREATE TABLE IF NOT EXISTS payroll_item_parameter_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    balance_policy_id BIGINT NOT NULL,
    parameter_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    input_type VARCHAR(20) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT FALSE,
    default_value VARCHAR(500) NULL,
    options_json JSON NULL,
    rule_parameter_flag BOOLEAN NOT NULL DEFAULT FALSE,
    daily_display_flag BOOLEAN NOT NULL DEFAULT FALSE,
    input_source_override_flag BOOLEAN NOT NULL DEFAULT FALSE,
    rule_value_resolver_key VARCHAR(100) NULL,
    display_order INT NOT NULL DEFAULT 0,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payroll_item_parameter_definition_key
        UNIQUE (tenant_id, balance_policy_id, parameter_key),
    CONSTRAINT fk_payroll_item_parameter_definition_policy
        FOREIGN KEY (balance_policy_id) REFERENCES payroll_item_balance_policy (id),
    CONSTRAINT chk_payroll_item_parameter_definition_type
        CHECK (input_type IN ('TEXT', 'NUMBER', 'SELECT', 'BOOLEAN', 'DATE'))
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

SET @parameter_resolver_key_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payroll_item_parameter_definition'
      AND column_name = 'rule_value_resolver_key'
);
SET @parameter_resolver_key_sql := IF(
    @parameter_resolver_key_exists = 0,
    'ALTER TABLE payroll_item_parameter_definition ADD COLUMN rule_value_resolver_key VARCHAR(100) NULL AFTER input_source_override_flag',
    'SELECT 1'
);
PREPARE statement FROM @parameter_resolver_key_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

-- Hibernate ddl-autoで先に作られたテーブルには複合UNIQUEがない場合がある。
-- 再適用前に既存重複を正規化し、以後のON DUPLICATE KEYを確実に機能させる。
DROP TEMPORARY TABLE IF EXISTS tmp_payroll_item_balance_policy_keep;
CREATE TEMPORARY TABLE tmp_payroll_item_balance_policy_keep AS
SELECT tenant_id, target_type, target_code, MIN(id) AS keep_id
FROM payroll_item_balance_policy
GROUP BY tenant_id, target_type, target_code;

UPDATE employee_payroll_item_enrollment enrollment
JOIN payroll_item_balance_policy policy
  ON policy.id = enrollment.balance_policy_id
JOIN tmp_payroll_item_balance_policy_keep canonical
  ON canonical.tenant_id = policy.tenant_id
 AND canonical.target_type = policy.target_type
 AND canonical.target_code = policy.target_code
SET enrollment.balance_policy_id = canonical.keep_id,
    enrollment.updated_at = NOW(6)
WHERE enrollment.balance_policy_id <> canonical.keep_id;

DELETE duplicate_enrollment
FROM employee_payroll_item_enrollment duplicate_enrollment
JOIN employee_payroll_item_enrollment keep_enrollment
  ON keep_enrollment.employee_id = duplicate_enrollment.employee_id
 AND keep_enrollment.balance_policy_id = duplicate_enrollment.balance_policy_id
 AND keep_enrollment.effective_to IS NULL
 AND keep_enrollment.deleted_at IS NULL
 AND keep_enrollment.id < duplicate_enrollment.id
WHERE duplicate_enrollment.effective_to IS NULL
  AND duplicate_enrollment.deleted_at IS NULL;

DELETE policy
FROM payroll_item_balance_policy policy
JOIN tmp_payroll_item_balance_policy_keep canonical
  ON canonical.tenant_id = policy.tenant_id
 AND canonical.target_type = policy.target_type
 AND canonical.target_code = policy.target_code
WHERE policy.id <> canonical.keep_id;

DROP TEMPORARY TABLE tmp_payroll_item_balance_policy_keep;

SET @balance_policy_unique_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'payroll_item_balance_policy'
      AND index_name = 'uk_payroll_item_balance_policy_code'
);
SET @balance_policy_unique_sql := IF(
    @balance_policy_unique_exists = 0,
    'ALTER TABLE payroll_item_balance_policy ADD CONSTRAINT uk_payroll_item_balance_policy_code UNIQUE (tenant_id, target_type, target_code)',
    'SELECT 1'
);
PREPARE statement FROM @balance_policy_unique_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @deduction_calculated_amount_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_deductions'
      AND column_name = 'calculated_amount'
);

-- 手当・控除で同じ基準額、上書き理由、数量、残高単位を保持する。
SET @allowance_calculated_amount_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_allowances'
      AND column_name = 'calculated_amount'
);
SET @allowance_calculated_amount_sql := IF(
    @allowance_calculated_amount_exists = 0,
    'ALTER TABLE daily_report_allowances ADD COLUMN calculated_amount INT NOT NULL DEFAULT 0 AFTER amount',
    'SELECT 1'
);
PREPARE statement FROM @allowance_calculated_amount_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @allowance_manual_override_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_allowances'
      AND column_name = 'manual_override_flag'
);
SET @allowance_manual_override_sql := IF(
    @allowance_manual_override_exists = 0,
    'ALTER TABLE daily_report_allowances ADD COLUMN manual_override_flag BOOLEAN NOT NULL DEFAULT FALSE AFTER calculated_amount',
    'SELECT 1'
);
PREPARE statement FROM @allowance_manual_override_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @allowance_override_reason_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_allowances'
      AND column_name = 'override_reason'
);
SET @allowance_override_reason_sql := IF(
    @allowance_override_reason_exists = 0,
    'ALTER TABLE daily_report_allowances ADD COLUMN override_reason VARCHAR(500) NULL AFTER manual_override_flag',
    'SELECT 1'
);
PREPARE statement FROM @allowance_override_reason_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @allowance_quantity_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_allowances'
      AND column_name = 'quantity'
);
SET @allowance_quantity_sql := IF(
    @allowance_quantity_exists = 0,
    'ALTER TABLE daily_report_allowances ADD COLUMN quantity DECIMAL(12,2) NULL AFTER override_reason',
    'SELECT 1'
);
PREPARE statement FROM @allowance_quantity_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @allowance_balance_unit_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report_allowances'
      AND column_name = 'balance_unit'
);
SET @allowance_balance_unit_sql := IF(
    @allowance_balance_unit_exists = 0,
    'ALTER TABLE daily_report_allowances ADD COLUMN balance_unit VARCHAR(20) NULL AFTER quantity',
    'SELECT 1'
);
PREPARE statement FROM @allowance_balance_unit_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

UPDATE daily_report_allowances
SET calculated_amount = amount
WHERE calculated_amount = 0
  AND amount <> 0;

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
    balance_unit, balance_tracking_flag, input_source,
    accrual_frequency, accrual_rule_name,
    carry_forward_flag, advance_consumption_flag, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT 'DEDUCTION', deduction.id, deduction.deduction_code, deduction.deduction_name,
       'DAYS', TRUE, 'DAILY_REPORT',
       'MONTHLY', 'CALENDAR_DAYS_IN_ENROLLMENT',
       TRUE, FALSE, TRUE,
       deduction.tenant_id, NOW(6), NOW(6), NULL
FROM deduction_masters deduction
WHERE deduction.deduction_code = 'DORMITORY_FEE'
  AND deduction.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    target_master_id = VALUES(target_master_id),
    display_name = VALUES(display_name),
    balance_unit = VALUES(balance_unit),
    balance_tracking_flag = VALUES(balance_tracking_flag),
    input_source = VALUES(input_source),
    accrual_frequency = VALUES(accrual_frequency),
    accrual_rule_name = VALUES(accrual_rule_name),
    updated_at = NOW(6),
    deleted_at = NULL;

-- 寮費固有の入力欄はアプリケーションへ直書きせず、定義データから生成する。
INSERT INTO payroll_item_parameter_definition (
    balance_policy_id, parameter_key, display_name, input_type,
    required_flag, default_value, options_json,
    rule_parameter_flag, daily_display_flag, input_source_override_flag,
    display_order, active_flag, tenant_id,
    created_at, updated_at, deleted_at
)
SELECT policy.id, 'dormitoryType', '寮タイプ', 'SELECT',
       TRUE, NULL,
       JSON_ARRAY(
           JSON_OBJECT(
               'label', '一人部屋',
               'value', 'SINGLE_ROOM',
               'calculationValue', COALESCE((
                   SELECT fee.daily_amount
                   FROM dormitory_fee_setting fee
                   WHERE fee.tenant_id = policy.tenant_id
                     AND fee.dormitory_type = 'SINGLE_ROOM'
                     AND fee.active_flag = TRUE
                     AND fee.deleted_at IS NULL
                   LIMIT 1
               ), 0)
           ),
           JSON_OBJECT(
               'label', '複数人部屋',
               'value', 'SHARED_ROOM',
               'calculationValue', COALESCE((
                   SELECT fee.daily_amount
                   FROM dormitory_fee_setting fee
                   WHERE fee.tenant_id = policy.tenant_id
                     AND fee.dormitory_type = 'SHARED_ROOM'
                     AND fee.active_flag = TRUE
                     AND fee.deleted_at IS NULL
                   LIMIT 1
               ), 0)
           )
       ),
       FALSE, FALSE, FALSE,
       10, TRUE, policy.tenant_id, NOW(6), NOW(6), NULL
FROM payroll_item_balance_policy policy
WHERE policy.target_type = 'DEDUCTION'
  AND policy.target_code = 'DORMITORY_FEE'
  AND policy.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name), input_type = VALUES(input_type),
    required_flag = VALUES(required_flag),
    options_json = CASE
        WHEN JSON_EXTRACT(
            payroll_item_parameter_definition.options_json,
            '$[0].calculationValue'
        ) IS NULL THEN VALUES(options_json)
        ELSE payroll_item_parameter_definition.options_json
    END,
    rule_parameter_flag = FALSE,
    input_source_override_flag = VALUES(input_source_override_flag),
    active_flag = TRUE, updated_at = NOW(6), deleted_at = NULL;

-- 選択肢の計算値を解決する汎用Resolverにより、寮費固有分岐をCoreへ置かない。
INSERT INTO payroll_item_parameter_definition (
    balance_policy_id, parameter_key, display_name, input_type,
    required_flag, default_value, options_json,
    rule_parameter_flag, daily_display_flag, input_source_override_flag,
    rule_value_resolver_key, display_order, active_flag, tenant_id,
    created_at, updated_at, deleted_at
)
SELECT policy.id, 'dormitoryDailyAmount', '寮費日額', 'NUMBER',
       TRUE, '0', NULL,
       TRUE, FALSE, FALSE,
       'SELECT_OPTION_CALCULATION_VALUE:dormitoryType', 15, TRUE, policy.tenant_id,
       NOW(6), NOW(6), NULL
FROM payroll_item_balance_policy policy
WHERE policy.target_type = 'DEDUCTION'
  AND policy.target_code = 'DORMITORY_FEE'
  AND policy.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name), input_type = VALUES(input_type),
    required_flag = VALUES(required_flag),
    rule_parameter_flag = TRUE,
    rule_value_resolver_key = VALUES(rule_value_resolver_key),
    display_order = VALUES(display_order), active_flag = TRUE,
    updated_at = NOW(6), deleted_at = NULL;

INSERT INTO payroll_item_parameter_definition (
    balance_policy_id, parameter_key, display_name, input_type,
    required_flag, default_value, options_json,
    rule_parameter_flag, daily_display_flag, input_source_override_flag,
    display_order, active_flag, tenant_id,
    created_at, updated_at, deleted_at
)
SELECT policy.id, 'inputSource', '徴収方式', 'SELECT',
       TRUE, 'DAILY_REPORT',
       JSON_ARRAY(
           JSON_OBJECT('label', '日報で日次徴収', 'value', 'DAILY_REPORT'),
           JSON_OBJECT('label', '月1回の一括徴収', 'value', 'TRANSACTION')
       ),
       FALSE, FALSE, TRUE,
       20, TRUE, policy.tenant_id, NOW(6), NOW(6), NULL
FROM payroll_item_balance_policy policy
WHERE policy.target_type = 'DEDUCTION'
  AND policy.target_code = 'DORMITORY_FEE'
  AND policy.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name), input_type = VALUES(input_type),
    required_flag = VALUES(required_flag), default_value = VALUES(default_value),
    options_json = VALUES(options_json),
    input_source_override_flag = VALUES(input_source_override_flag),
    active_flag = TRUE, updated_at = NOW(6), deleted_at = NULL;

-- 旧設定値は共通入力元コードへ移行する。
UPDATE employee_payroll_item_enrollment enrollment
JOIN payroll_item_balance_policy policy ON policy.id = enrollment.balance_policy_id
SET enrollment.settings_json = JSON_SET(
        COALESCE(enrollment.settings_json, JSON_OBJECT()),
        '$.inputSource',
        CASE JSON_UNQUOTE(JSON_EXTRACT(enrollment.settings_json, '$.collectionMode'))
            WHEN 'MONTHLY' THEN 'TRANSACTION'
            ELSE 'DAILY_REPORT'
        END
    ),
    enrollment.updated_at = NOW(6)
WHERE policy.target_type = 'DEDUCTION'
  AND policy.target_code = 'DORMITORY_FEE'
  AND policy.deleted_at IS NULL
  AND enrollment.deleted_at IS NULL
  AND JSON_EXTRACT(enrollment.settings_json, '$.inputSource') IS NULL;

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
       0, TRUE, 'MONTHLY', 'NONE', FALSE, TRUE, TRUE, 120, TRUE,
       '明細到着時に従業員別控除取引として登録', 'default', NOW(6), NOW(6), NULL
WHERE NOT EXISTS (
    SELECT 1 FROM deduction_masters
    WHERE tenant_id = 'default' AND deduction_code = 'MOBILE_RENTAL'
      AND deleted_at IS NULL
);

UPDATE deduction_masters
SET deduction_unit = 'MONTHLY',
    show_on_daily_statement = FALSE,
    show_on_monthly_statement = TRUE,
    carry_to_monthly_settlement = TRUE,
    allow_manual_input = TRUE,
    note = '明細到着時に従業員別控除取引として登録',
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
    balance_unit, balance_tracking_flag, input_source,
    accrual_frequency, accrual_rule_name,
    carry_forward_flag, advance_consumption_flag, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT 'DEDUCTION', deduction.id, deduction.deduction_code, deduction.deduction_name,
       'DAYS', FALSE, 'TRANSACTION',
       'MANUAL', 'MANUAL_TRANSACTION',
       TRUE, FALSE, TRUE,
       deduction.tenant_id, NOW(6), NOW(6), NULL
FROM deduction_masters deduction
WHERE deduction.deduction_code = 'MOBILE_RENTAL'
  AND deduction.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    target_master_id = VALUES(target_master_id),
    display_name = VALUES(display_name),
    balance_tracking_flag = VALUES(balance_tracking_flag),
    input_source = VALUES(input_source),
    accrual_frequency = VALUES(accrual_frequency),
    accrual_rule_name = VALUES(accrual_rule_name),
    active_flag = TRUE,
    updated_at = NOW(6);
