-- ProjectAdminSystem V1
-- ローカルDocker専用：月間集計表の動作確認fixture
--
-- 本番環境では適用しない。
-- 同じコード・名称・対象日を再利用し、Docker再起動時も重複させない。

SET @fixture_tenant_id = 'default';
SET @fixture_customer_name = 'E2E 月間集計検証顧客';
SET @fixture_site_name = 'E2E 東京検証現場';
SET @fixture_employee_code = 'E2E-EMP-001';
SET @fixture_work_date = '2026-08-10';
SET @fixture_job_code = 'E2E_GENERAL_WORK';
SET @fixture_job_name = 'E2E 一般作業員';

SET @fixture_customer_id = (
    SELECT MIN(id)
    FROM customers
    WHERE tenant_id = @fixture_tenant_id
      AND name = @fixture_customer_name
);

INSERT INTO customers (
    tenant_id, created_at, updated_at, deleted_at,
    name, furigana_name, short_name,
    post_no, address, representative_name, phone,
    job_type, contract_flag, invoice_type,
    closing_day_type, closing_day_value, closing_month_offset,
    payment_day_type, payment_day_value, payment_month_offset
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_customer_name, 'いーつーいーげつかんしゅうけいけんしょうこきゃく', 'E2E検証顧客',
    '100-0001', '東京都千代田区E2E 1-1', 'E2E 担当者', '03-0000-0000',
    '建設', '契約中', 'PATTERN_1',
    'END_OF_MONTH', NULL, 0,
    'DAY_OF_MONTH', 25, 1
WHERE @fixture_customer_id IS NULL;

SET @fixture_customer_id = COALESCE(
    @fixture_customer_id,
    LAST_INSERT_ID()
);

UPDATE customers
SET deleted_at = NULL,
    short_name = 'E2E検証顧客',
    invoice_type = 'PATTERN_1',
    closing_day_type = 'END_OF_MONTH',
    closing_day_value = NULL,
    closing_month_offset = 0,
    payment_day_type = 'DAY_OF_MONTH',
    payment_day_value = 25,
    payment_month_offset = 1,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE id = @fixture_customer_id;

SET @fixture_site_id = (
    SELECT MIN(id)
    FROM customer_sites
    WHERE tenant_id = @fixture_tenant_id
      AND customer_id = @fixture_customer_id
      AND name = @fixture_site_name
);

INSERT INTO customer_sites (
    tenant_id, created_at, updated_at, deleted_at,
    customer_id, name,
    contact_person_name, contact_person_phone,
    contact_person_email, distance_from_company_km
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_customer_id, @fixture_site_name,
    'E2E 現場責任者', '090-0000-0000',
    'e2e-site@example.invalid', 15
WHERE @fixture_site_id IS NULL;

SET @fixture_site_id = COALESCE(
    @fixture_site_id,
    LAST_INSERT_ID()
);

UPDATE customer_sites
SET deleted_at = NULL,
    distance_from_company_km = 15,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE id = @fixture_site_id;

INSERT INTO customer_site_billing_rates (
    tenant_id, created_at, updated_at, deleted_at,
    customer_site_id,
    job_code, job_name,
    site_role_code, site_role_name,
    billing_unit,
    base_unit_price, overtime_unit_price,
    night_unit_price, holiday_unit_price,
    commute_unit_price,
    effective_from, effective_to,
    display_order, active_flag, note
) VALUES (
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_site_id,
    @fixture_job_code, @fixture_job_name,
    'GENERAL', '一般',
    'DAILY',
    22000, 2750,
    3300, 29700,
    30,
    '2026-04-01', NULL,
    10, TRUE, 'ローカル月間集計表確認用の日単価'
)
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name),
    site_role_name = VALUES(site_role_name),
    billing_unit = 'DAILY',
    base_unit_price = VALUES(base_unit_price),
    overtime_unit_price = VALUES(overtime_unit_price),
    night_unit_price = VALUES(night_unit_price),
    holiday_unit_price = VALUES(holiday_unit_price),
    commute_unit_price = VALUES(commute_unit_price),
    effective_to = NULL,
    active_flag = TRUE,
    deleted_at = NULL,
    note = VALUES(note),
    updated_at = CURRENT_TIMESTAMP(6);

SET @fixture_billing_rate_id = (
    SELECT id
    FROM customer_site_billing_rates
    WHERE tenant_id = @fixture_tenant_id
      AND customer_site_id = @fixture_site_id
      AND job_code = @fixture_job_code
      AND site_role_code = 'GENERAL'
      AND effective_from = '2026-04-01'
      AND deleted_at IS NULL
    ORDER BY id DESC
    LIMIT 1
);

INSERT INTO employee (
    tenant_id, created_at, updated_at, deleted_at,
    employee_code, employee_name, employee_name_kana,
    birth_date, hire_date,
    employment_type, employment_status,
    email, dormitory_flag, dormitory_type, active_flag
) VALUES (
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_employee_code, 'E2E 給与検証社員', 'イーツーイー キュウヨケンショウシャイン',
    '1990-01-15', '2026-04-01',
    'FULL_TIME', 'ACTIVE',
    'e2e-employee@example.invalid', TRUE, 'SHARED_ROOM', TRUE
)
ON DUPLICATE KEY UPDATE
    employee_name = VALUES(employee_name),
    employee_name_kana = VALUES(employee_name_kana),
    employment_status = 'ACTIVE',
    email = VALUES(email),
    dormitory_flag = TRUE,
    dormitory_type = 'SHARED_ROOM',
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
    FALSE, 'HOURLY', 'MONTHLY',
    0, 0, 0, 1500,
    40, 'ローカル月間集計表確認用'
)
ON DUPLICATE KEY UPDATE
    contract_start_date = VALUES(contract_start_date),
    contract_end_date = NULL,
    salary_type = 'HOURLY',
    payment_cycle = 'MONTHLY',
    hourly_wage = 1500,
    note = VALUES(note),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO employee_payroll_profile (
    tenant_id, created_at, updated_at, deleted_at,
    employee_id, tax_category, tax_dependent_count,
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
    @fixture_employee_id, 'KOU', 0,
    FALSE, FALSE,
    10,
    TRUE, TRUE,
    0,
    TRUE, TRUE,
    TRUE, TRUE,
    FALSE, FALSE,
    0
)
ON DUPLICATE KEY UPDATE
    tax_category = 'KOU',
    resident_tax_calc_flag = TRUE,
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

SET @fixture_daily_report_id = (
    SELECT MIN(id)
    FROM daily_report
    WHERE tenant_id = @fixture_tenant_id
      AND employee_id = @fixture_employee_id
      AND work_date = @fixture_work_date
);

INSERT INTO daily_report (
    tenant_id, created_at, updated_at, deleted_at,
    employee_id, work_date, payment_date,
    customer_id, customer_site_id,
    customer_name, site_name,
    billing_rate_id, billing_unit,
    job_code, job_name,
    site_role_code, site_role_name,
    billing_base_unit_price,
    billing_overtime_unit_price,
    billing_night_unit_price,
    billing_holiday_unit_price,
    billing_commute_unit_price,
    work_description,
    start_time, end_time, break_minutes,
    work_hours, overtime_hours,
    night_work_hours, holiday_work_hours,
    holiday_premium_eligible,
    allowance_amount, deduction_amount,
    loan_repayment_amount, saving_amount,
    dormitory_charge_days,
    normal_pay_amount, overtime_pay_amount,
    night_pay_amount, holiday_pay_amount,
    estimated_gross_pay_amount,
    estimated_net_pay_amount,
    vehicle_used_flag, mileage, paid_leave_days,
    approval_status, approval_comment
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_employee_id, @fixture_work_date, @fixture_work_date,
    @fixture_customer_id, @fixture_site_id,
    @fixture_customer_name, @fixture_site_name,
    @fixture_billing_rate_id, 'DAILY',
    @fixture_job_code, @fixture_job_name,
    'GENERAL', '一般',
    22000, 2750,
    3300, 29700,
    30,
    'ローカル月間集計表の動作確認',
    '08:00:00', '18:00:00', 60,
    8, 2,
    0, 0,
    FALSE,
    0, 2350,
    0, 0,
    3,
    12000, 3000,
    0, 0,
    15000,
    12650,
    FALSE, 0, 0,
    'APPROVED', 'ローカルfixture自動承認'
WHERE @fixture_daily_report_id IS NULL;

SET @fixture_daily_report_id = COALESCE(
    @fixture_daily_report_id,
    LAST_INSERT_ID()
);

UPDATE daily_report
SET deleted_at = NULL,
    payment_date = @fixture_work_date,
    customer_id = @fixture_customer_id,
    customer_site_id = @fixture_site_id,
    customer_name = @fixture_customer_name,
    site_name = @fixture_site_name,
    billing_rate_id = @fixture_billing_rate_id,
    billing_unit = 'DAILY',
    job_code = @fixture_job_code,
    job_name = @fixture_job_name,
    site_role_code = 'GENERAL',
    site_role_name = '一般',
    billing_base_unit_price = 22000,
    billing_overtime_unit_price = 2750,
    billing_night_unit_price = 3300,
    billing_holiday_unit_price = 29700,
    billing_commute_unit_price = 30,
    approval_status = 'APPROVED',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE id = @fixture_daily_report_id;

SELECT
    @fixture_customer_id AS customer_id,
    @fixture_site_id AS customer_site_id,
    @fixture_billing_rate_id AS billing_rate_id,
    @fixture_employee_id AS employee_id,
    @fixture_daily_report_id AS daily_report_id;
