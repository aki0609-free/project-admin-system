-- ProjectAdminSystem V1
-- ローカルDocker専用：月次給与・月次Excel帳票の動作確認fixture
--
-- 本番環境では適用しない。
-- 税率マスターへ仮の法定値を登録せず、帳票ファイル生成に必要な
-- 月給契約・会社・締め期間・支払日設定だけを再実行可能な形で用意する。

SET @fixture_tenant_id = 'default';
SET @fixture_employee_code = 'E2E-MONTHLY-001';
SET @fixture_target_month = '2026-08-01';

INSERT INTO company_profile (
    tenant_id, created_at, updated_at, deleted_at,
    company_code, company_name, short_name, active_flag
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    'LOCAL-E2E', 'E2E ローカル検証会社', 'E2E検証会社', TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM company_profile
    WHERE tenant_id = @fixture_tenant_id
      AND deleted_at IS NULL
);

INSERT INTO closing_setting (
    tenant_id, created_at, updated_at, deleted_at,
    setting_code,
    closing_day_type, closing_day_value, closing_month_offset,
    payment_day_type, payment_day_value, payment_month_offset,
    active_flag
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    'PAYROLL',
    'END_OF_MONTH', NULL, 0,
    'DAY_OF_MONTH', 15, 1,
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM closing_setting
    WHERE tenant_id = @fixture_tenant_id
      AND setting_code = 'PAYROLL'
      AND deleted_at IS NULL
);

INSERT INTO monthly_closings (
    tenant_id, created_at, updated_at, deleted_at,
    target_month,
    closing_start_date, closing_end_date,
    status, closing_version, note
) VALUES (
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_target_month,
    '2026-08-01', '2026-08-31',
    'OPEN', 0, 'ローカル月次帳票E2E用'
)
ON DUPLICATE KEY UPDATE
    closing_start_date = VALUES(closing_start_date),
    closing_end_date = VALUES(closing_end_date),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO employee (
    tenant_id, created_at, updated_at, deleted_at,
    employee_code, employee_name, employee_name_kana,
    birth_date, hire_date,
    employment_type, employment_status,
    email, dormitory_flag, active_flag
) VALUES (
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_employee_code, 'E2E 月次帳票検証社員',
    'イーツーイー ゲツジチョウヒョウケンショウシャイン',
    '1991-02-15', '2026-04-01',
    'FULL_TIME', 'ACTIVE',
    'e2e-monthly@example.invalid', FALSE, TRUE
)
ON DUPLICATE KEY UPDATE
    employee_name = VALUES(employee_name),
    employee_name_kana = VALUES(employee_name_kana),
    employment_status = 'ACTIVE',
    active_flag = TRUE,
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

SET @fixture_employee_id = (
    SELECT id
    FROM employee
    WHERE tenant_id = @fixture_tenant_id
      AND employee_code = @fixture_employee_code
    LIMIT 1
);

INSERT INTO employee_contract (
    tenant_id, created_at, updated_at, deleted_at,
    employee_id, contract_start_date, contract_end_date,
    renewal_flag, salary_type, payment_cycle,
    monthly_salary, weekly_wage, daily_wage, hourly_wage,
    standard_working_hours, note
) VALUES (
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_employee_id, '2026-04-01', NULL,
    FALSE, 'MONTHLY', 'MONTHLY',
    300000, 0, 0, 0,
    40, 'ローカル月次帳票E2E用'
)
ON DUPLICATE KEY UPDATE
    contract_start_date = VALUES(contract_start_date),
    contract_end_date = NULL,
    salary_type = 'MONTHLY',
    payment_cycle = 'MONTHLY',
    monthly_salary = 300000,
    weekly_wage = 0,
    daily_wage = 0,
    hourly_wage = 0,
    deleted_at = NULL,
    note = VALUES(note),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO employee_payroll_profile (
    tenant_id, created_at, updated_at, deleted_at,
    employee_id,
    tax_category, tax_dependent_count,
    dependent_flag, dependent_of_other_flag,
    paid_leave_remaining_days,
    income_tax_calc_flag, resident_tax_calc_flag,
    resident_tax_monthly,
    employment_insurance_flag, social_insurance_flag,
    health_insurance_flag, pension_insurance_flag,
    care_insurance_flag, daily_pay_flag,
    commute_allowance_monthly
) VALUES (
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_employee_id,
    'KOU', 0,
    FALSE, FALSE, 10,
    FALSE, FALSE, 0,
    FALSE, FALSE,
    FALSE, FALSE,
    FALSE, FALSE, 0
)
ON DUPLICATE KEY UPDATE
    tax_category = 'KOU',
    tax_dependent_count = 0,
    income_tax_calc_flag = FALSE,
    resident_tax_calc_flag = FALSE,
    employment_insurance_flag = FALSE,
    social_insurance_flag = FALSE,
    health_insurance_flag = FALSE,
    pension_insurance_flag = FALSE,
    care_insurance_flag = FALSE,
    daily_pay_flag = FALSE,
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO payroll_calculation_period (
    target_month,
    income_tax_year, insurance_rate_year,
    child_care_support_required, rounding_mode,
    verified_flag, verified_at, verified_by, source_note,
    tenant_id, created_at, updated_at, deleted_at
) VALUES (
    @fixture_target_month,
    2026, 2026,
    FALSE, 'HALF_UP',
    TRUE, CURRENT_TIMESTAMP(6), 'local-e2e',
    'ローカル月次帳票のファイル生成確認用。法定税率の検証には使用しない。',
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
)
ON DUPLICATE KEY UPDATE
    verified_flag = IF(verified_by = 'local-e2e', TRUE, verified_flag),
    verified_at = IF(verified_by = 'local-e2e', CURRENT_TIMESTAMP(6), verified_at),
    deleted_at = IF(verified_by = 'local-e2e', NULL, deleted_at),
    updated_at = CURRENT_TIMESTAMP(6);

-- 月次画面の印刷・ダウンロードは、締め時点の確定履歴から再出力する。
-- 他のE2Eが月給社員を登録している途中でも最新View全体の状態に影響されないよう、
-- この固定社員だけをローカル検証Versionへ一度だけスナップショットする。
INSERT INTO monthly_labor_cost_list_history (
    target_month, closing_version,
    employee_id, employee_code, employee_name, company_name,
    period_from, period_to, payment_date, business_key,
    work_day_count, paid_leave_days, overtime_hours, night_work_hours,
    basic_salary, overtime_pay_amount, night_pay_amount,
    driver_allowance_amount, other_allowance_amount,
    business_trip_allowance_amount, gross_amount,
    health_insurance, child_care_contribution, pension_insurance,
    employment_insurance, social_insurance_total, taxable_amount,
    income_tax, year_end_adjustment_amount, resident_tax,
    dormitory_fee_amount, mobile_rental_amount, wifi_fee_amount,
    other_deduction_amount, deduction_total,
    net_before_advance_amount, advance_payment_amount,
    saving_amount, net_payment_amount,
    source_view_name, source_execution_id, fixed_at,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    source.target_month, 900001,
    source.employee_id, source.employee_code,
    source.employee_name, source.company_name,
    source.period_from, source.period_to,
    source.payment_date, source.business_key,
    source.work_day_count, source.paid_leave_days,
    source.overtime_hours, source.night_work_hours,
    source.basic_salary, source.overtime_pay_amount,
    source.night_pay_amount, source.driver_allowance_amount,
    source.other_allowance_amount,
    source.business_trip_allowance_amount, source.gross_amount,
    source.health_insurance, source.child_care_contribution,
    source.pension_insurance, source.employment_insurance,
    source.social_insurance_total, source.taxable_amount,
    source.income_tax, source.year_end_adjustment_amount,
    source.resident_tax, source.dormitory_fee_amount,
    source.mobile_rental_amount, source.wifi_fee_amount,
    source.other_deduction_amount, source.deduction_total,
    source.net_before_advance_amount,
    source.advance_payment_amount, source.saving_amount,
    source.net_payment_amount,
    'vw_monthly_labor_cost_list_latest',
    'LOCAL-E2E-MONTHLY-LABOR-COST-V900001',
    CURRENT_TIMESTAMP(6),
    source.tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM vw_monthly_labor_cost_list_latest source
WHERE source.tenant_id = @fixture_tenant_id
  AND source.employee_code = @fixture_employee_code
  AND source.target_month = @fixture_target_month
  AND source.calculation_ready = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM monthly_labor_cost_list_history history
      WHERE history.tenant_id = @fixture_tenant_id
        AND history.target_month = @fixture_target_month
        AND history.closing_version = 900001
        AND history.employee_id = @fixture_employee_id
        AND history.deleted_at IS NULL
  );
