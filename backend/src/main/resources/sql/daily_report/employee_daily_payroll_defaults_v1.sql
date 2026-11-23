-- ProjectAdmin / Fuyo
-- 従業員別の日報初期額（勤務態度手当・管理手当・法定準備金）。
-- Ruleに従業員コードを直書きせず、有効期間付き設定値をRuleパラメータとして渡す。

SET NAMES utf8mb4;

INSERT INTO rule_master (
    rule_name, rule_display_name, rule_type, dsl_type, dsl_text,
    rule_bean_name, result_fact_key, description, priority, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
VALUES (
    'EMPLOYEE_CONFIGURED_DAILY_AMOUNT',
    '従業員別日報初期額',
    'ALLOWANCE',
    'JEXL',
    'employeeDailyAmount',
    NULL,
    'result',
    '従業員の手当・控除設定に保存した日報初期額を返す。',
    100,
    TRUE,
    'default',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
)
ON DUPLICATE KEY UPDATE
    rule_display_name = VALUES(rule_display_name),
    rule_type = VALUES(rule_type),
    dsl_type = VALUES(dsl_type),
    dsl_text = VALUES(dsl_text),
    rule_bean_name = NULL,
    result_fact_key = VALUES(result_fact_key),
    description = VALUES(description),
    active_flag = TRUE,
    deleted_at = NULL,
    updated_at = VALUES(updated_at);

INSERT INTO rule_parameter (
    rule_id, param_name, data_type, required_flag, default_value,
    description, order_no, tenant_id, created_at, updated_at, deleted_at
)
SELECT
    rule.id,
    'employeeDailyAmount',
    'DECIMAL',
    TRUE,
    '0',
    '従業員別の日報初期額',
    1,
    rule.tenant_id,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM rule_master rule
WHERE rule.tenant_id = 'default'
  AND rule.rule_name = 'EMPLOYEE_CONFIGURED_DAILY_AMOUNT'
  AND rule.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM rule_parameter existing
      WHERE existing.rule_id = rule.id
        AND existing.param_name = 'employeeDailyAmount'
        AND existing.deleted_at IS NULL
  );

-- 0円は「従業員別固定額なし」を表し、既存の予測法定控除÷20へフォールバックする。
UPDATE rule_master
SET dsl_text = 'employeeDailyAmount > 0 ? employeeDailyAmount : predictedStatutoryDeductionTotal / 20',
    description = '従業員別日報初期額を優先し、未設定時は月額予測法定控除合計を20日で按分する。',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE tenant_id = 'default'
  AND rule_name = 'DAILY_LEGAL_DEPOSIT_ESTIMATE'
  AND deleted_at IS NULL;

INSERT INTO rule_parameter (
    rule_id, param_name, data_type, required_flag, default_value,
    description, order_no, tenant_id, created_at, updated_at, deleted_at
)
SELECT
    rule.id,
    'employeeDailyAmount',
    'DECIMAL',
    TRUE,
    '0',
    '従業員別の法定準備金日報初期額（0円は予測額を使用）',
    2,
    rule.tenant_id,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM rule_master rule
WHERE rule.tenant_id = 'default'
  AND rule.rule_name = 'DAILY_LEGAL_DEPOSIT_ESTIMATE'
  AND rule.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM rule_parameter existing
      WHERE existing.rule_id = rule.id
        AND existing.param_name = 'employeeDailyAmount'
        AND existing.deleted_at IS NULL
  );

UPDATE allowance_masters
SET calculation_type = 'AUTO',
    rule_name = 'EMPLOYEE_CONFIGURED_DAILY_AMOUNT',
    default_amount = 0,
    allow_manual_input = TRUE,
    min_amount = 0,
    note = '従業員別の日報初期額を表示し、日報上で手動変更できる。',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE tenant_id = 'default'
  AND allowance_code IN ('ATTENDANCE_ATTITUDE', 'MANAGEMENT_ALLOWANCE')
  AND deleted_at IS NULL;

INSERT INTO payroll_item_balance_policy (
    target_type, target_master_id, target_code, display_name,
    application_scope, balance_unit, balance_tracking_flag, input_source,
    accrual_frequency, accrual_rule_name,
    carry_forward_flag, advance_consumption_flag, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    'ALLOWANCE', allowance.id, allowance.allowance_code, allowance.allowance_name,
    'EMPLOYEE_ENROLLMENT', 'AMOUNT', FALSE, 'DAILY_REPORT',
    'MANUAL', 'NONE',
    FALSE, FALSE, TRUE,
    allowance.tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM allowance_masters allowance
WHERE allowance.tenant_id = 'default'
  AND allowance.allowance_code IN ('ATTENDANCE_ATTITUDE', 'MANAGEMENT_ALLOWANCE')
  AND allowance.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    target_master_id = VALUES(target_master_id),
    display_name = VALUES(display_name),
    application_scope = VALUES(application_scope),
    balance_unit = VALUES(balance_unit),
    balance_tracking_flag = VALUES(balance_tracking_flag),
    input_source = VALUES(input_source),
    accrual_frequency = VALUES(accrual_frequency),
    accrual_rule_name = VALUES(accrual_rule_name),
    carry_forward_flag = VALUES(carry_forward_flag),
    advance_consumption_flag = VALUES(advance_consumption_flag),
    active_flag = TRUE,
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL;

INSERT INTO payroll_item_balance_policy (
    target_type, target_master_id, target_code, display_name,
    application_scope, balance_unit, balance_tracking_flag, input_source,
    accrual_frequency, accrual_rule_name,
    carry_forward_flag, advance_consumption_flag, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    'DEDUCTION', deduction.id, deduction.deduction_code, deduction.deduction_name,
    'EMPLOYEE_ENROLLMENT', 'AMOUNT', FALSE, 'DAILY_REPORT',
    'MANUAL', 'NONE',
    FALSE, FALSE, TRUE,
    deduction.tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM deduction_masters deduction
WHERE deduction.tenant_id = 'default'
  AND deduction.deduction_code = 'LEGAL_DEPOSIT'
  AND deduction.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    target_master_id = VALUES(target_master_id),
    display_name = VALUES(display_name),
    application_scope = VALUES(application_scope),
    balance_unit = VALUES(balance_unit),
    balance_tracking_flag = VALUES(balance_tracking_flag),
    input_source = VALUES(input_source),
    accrual_frequency = VALUES(accrual_frequency),
    accrual_rule_name = VALUES(accrual_rule_name),
    carry_forward_flag = VALUES(carry_forward_flag),
    advance_consumption_flag = VALUES(advance_consumption_flag),
    active_flag = TRUE,
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL;

INSERT INTO payroll_item_parameter_definition (
    balance_policy_id, parameter_key, display_name, input_type,
    required_flag, default_value, options_json,
    rule_parameter_flag, daily_display_flag, input_source_override_flag,
    rule_value_resolver_key, display_order, active_flag, tenant_id,
    created_at, updated_at, deleted_at
)
SELECT
    policy.id, 'employeeDailyAmount', '日報初期額', 'NUMBER',
    TRUE, '0', NULL,
    TRUE, FALSE, FALSE,
    NULL, 10, TRUE, policy.tenant_id,
    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM payroll_item_balance_policy policy
WHERE policy.tenant_id = 'default'
  AND policy.target_code IN (
      'ATTENDANCE_ATTITUDE',
      'MANAGEMENT_ALLOWANCE',
      'LEGAL_DEPOSIT'
  )
  AND policy.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    input_type = VALUES(input_type),
    required_flag = VALUES(required_flag),
    default_value = VALUES(default_value),
    rule_parameter_flag = TRUE,
    daily_display_flag = FALSE,
    input_source_override_flag = FALSE,
    rule_value_resolver_key = NULL,
    display_order = VALUES(display_order),
    active_flag = TRUE,
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL;

-- 法定準備金は従来から全従業員に表示されていたため、移行時に表示を落とさない。
-- 0円設定により、既存の予測法定控除÷20がそのまま初期値になる。
INSERT INTO employee_payroll_item_enrollment (
    employee_id, balance_policy_id, effective_from, effective_to,
    settings_json, tenant_id, created_at, updated_at, deleted_at
)
SELECT
    employee.id,
    policy.id,
    DATE('1900-01-01'),
    NULL,
    JSON_OBJECT('employeeDailyAmount', '0'),
    employee.tenant_id,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM employee employee
JOIN payroll_item_balance_policy policy
  ON policy.tenant_id = employee.tenant_id
 AND policy.target_type = 'DEDUCTION'
 AND policy.target_code = 'LEGAL_DEPOSIT'
 AND policy.deleted_at IS NULL
WHERE employee.tenant_id = 'default'
  AND employee.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM employee_payroll_item_enrollment existing
      WHERE existing.tenant_id = employee.tenant_id
        AND existing.employee_id = employee.id
        AND existing.balance_policy_id = policy.id
        AND existing.effective_to IS NULL
        AND existing.deleted_at IS NULL
  );
