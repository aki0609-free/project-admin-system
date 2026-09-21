-- ProjectAdminSystem V1
-- Local Docker only: 2026-08 month-end closing / monthly pay slip fixture.
--
-- The fixture is intentionally idempotent. It creates eight employees covering
-- salary bases and payment cycles, twenty August business-day reports per
-- employee, dynamic allowances/deductions, savings, loans and resident tax.
-- Production runtime schema must never include this file.

SET @fixture_tenant_id = 'default';
SET @fixture_target_month = '2026-08-01';

-- September-only daily-operation employees must not enter the August close.
UPDATE employee
SET hire_date = '2026-09-01',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE tenant_id = @fixture_tenant_id
  AND (
      employee_code LIKE 'E2E-DAILY-%'
      OR employee_code LIKE 'E2E-WEEKLY-%'
      OR employee_code LIKE 'E2E-MONTHLY-10%'
      OR employee_code LIKE 'E2E-MONTHLY-DAILY-%'
  );

-- The monthly-summary fixture also belongs to August. Disable statutory
-- calculations for this local-only employee because official tax/rate master
-- import is verified separately and this fixture must not invent legal rates.
UPDATE employee_payroll_profile profile
JOIN employee employee
  ON employee.id = profile.employee_id
SET profile.income_tax_calc_flag = FALSE,
    profile.employment_insurance_flag = FALSE,
    profile.social_insurance_flag = FALSE,
    profile.health_insurance_flag = FALSE,
    profile.pension_insurance_flag = FALSE,
    profile.care_insurance_flag = FALSE,
    profile.resident_tax_calc_flag = FALSE,
    profile.updated_at = CURRENT_TIMESTAMP(6)
WHERE employee.tenant_id = @fixture_tenant_id
  AND employee.employee_code = 'E2E-EMP-001'
  AND profile.deleted_at IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_august_employees;
CREATE TEMPORARY TABLE tmp_august_employees (
    employee_code varchar(100) primary key,
    employee_id bigint null,
    employee_name varchar(200) not null,
    employee_name_kana varchar(200) not null,
    payment_cycle varchar(20) not null,
    salary_type varchar(20) not null,
    monthly_salary decimal(12,2) not null,
    weekly_wage decimal(12,2) not null,
    daily_wage decimal(12,2) not null,
    resident_tax int not null,
    sort_order int not null
);

INSERT INTO tmp_august_employees VALUES
    ('E2E-AUG-D-001', NULL, '八月日次 青木 一郎', 'ハチガツニチジ アオキ イチロウ', 'DAILY', 'DAILY', 0, 0, 12000, 6200, 1),
    ('E2E-AUG-D-002', NULL, '八月日次 伊藤 二郎', 'ハチガツニチジ イトウ ジロウ', 'DAILY', 'DAILY', 0, 0, 13500, 7100, 2),
    ('E2E-AUG-WD-001', NULL, '八月週次日給 山本 一郎', 'ハチガツシュウジニッキュウ ヤマモト イチロウ', 'WEEKLY', 'DAILY', 0, 0, 13000, 7600, 3),
    ('E2E-AUG-WD-002', NULL, '八月週次日給 吉田 二郎', 'ハチガツシュウジニッキュウ ヨシダ ジロウ', 'WEEKLY', 'DAILY', 0, 0, 14500, 8300, 4),
    ('E2E-AUG-WW-001', NULL, '八月週次週給 佐藤 一郎', 'ハチガツシュウジシュウキュウ サトウ イチロウ', 'WEEKLY', 'WEEKLY', 0, 70000, 0, 10500, 5),
    ('E2E-AUG-MD-001', NULL, '八月月次日給 鈴木 一郎', 'ハチガツゲツジニッキュウ スズキ イチロウ', 'MONTHLY', 'DAILY', 0, 0, 14000, 8000, 6),
    ('E2E-AUG-MD-002', NULL, '八月月次日給 高橋 二郎', 'ハチガツゲツジニッキュウ タカハシ ジロウ', 'MONTHLY', 'DAILY', 0, 0, 15500, 9000, 7),
    ('E2E-AUG-MM-001', NULL, '八月月次月給 中村 一郎', 'ハチガツゲツジゲッキュウ ナカムラ イチロウ', 'MONTHLY', 'MONTHLY', 300000, 0, 0, 12000, 8);

INSERT INTO employee (
    tenant_id, created_at, updated_at, deleted_at,
    employee_code, employee_name, employee_name_kana,
    birth_date, hire_date, resign_date,
    employment_type, employment_status,
    phone, email, dormitory_flag, dormitory_type, active_flag
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    fixture.employee_code, fixture.employee_name, fixture.employee_name_kana,
    DATE_ADD('1980-01-01', INTERVAL fixture.sort_order YEAR),
    '2026-08-01', '2026-08-31',
    'FULL_TIME', 'RESIGNED',
    '090-8080-0000', CONCAT(LOWER(fixture.employee_code), '@example.invalid'),
    fixture.sort_order IN (1, 2),
    IF(fixture.sort_order IN (1, 2), 'SHARED_ROOM', NULL), TRUE
FROM tmp_august_employees fixture
ON DUPLICATE KEY UPDATE
    employee_name = VALUES(employee_name),
    employee_name_kana = VALUES(employee_name_kana),
    hire_date = VALUES(hire_date),
    resign_date = VALUES(resign_date),
    employment_status = VALUES(employment_status),
    active_flag = TRUE,
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

UPDATE tmp_august_employees fixture
JOIN employee employee
  ON employee.tenant_id = @fixture_tenant_id
 AND employee.employee_code = fixture.employee_code
SET fixture.employee_id = employee.id;

INSERT INTO employee_contract (
    tenant_id, created_at, updated_at, deleted_at,
    employee_id, contract_start_date, contract_end_date,
    renewal_flag, salary_type, payment_cycle,
    monthly_salary, weekly_wage, daily_wage, hourly_wage,
    standard_working_hours, note
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    fixture.employee_id, '2026-08-01', '2026-08-31',
    FALSE, fixture.salary_type, fixture.payment_cycle,
    fixture.monthly_salary, fixture.weekly_wage, fixture.daily_wage, 0,
    40, '2026年8月Local月次締め検証用'
FROM tmp_august_employees fixture
ON DUPLICATE KEY UPDATE
    contract_start_date = VALUES(contract_start_date),
    contract_end_date = VALUES(contract_end_date),
    salary_type = VALUES(salary_type),
    payment_cycle = VALUES(payment_cycle),
    monthly_salary = VALUES(monthly_salary),
    weekly_wage = VALUES(weekly_wage),
    daily_wage = VALUES(daily_wage),
    hourly_wage = 0,
    note = VALUES(note),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO employee_payroll_profile (
    tenant_id, created_at, updated_at, deleted_at,
    employee_id, tax_category, tax_dependent_count,
    dependent_flag, dependent_of_other_flag,
    paid_leave_remaining_days,
    income_tax_calc_flag, resident_tax_calc_flag, resident_tax_monthly,
    employment_insurance_flag, social_insurance_flag,
    health_insurance_flag, pension_insurance_flag,
    care_insurance_flag, daily_pay_flag, commute_allowance_monthly
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    fixture.employee_id, 'KOU', MOD(fixture.sort_order, 3),
    MOD(fixture.sort_order, 3) > 0, FALSE, 10,
    TRUE, fixture.resident_tax > 0, 0,
    TRUE, TRUE, TRUE, TRUE, fixture.sort_order <= 6,
    fixture.payment_cycle = 'DAILY',
    IF(fixture.sort_order IN (3, 6), 5000, 0)
FROM tmp_august_employees fixture
ON DUPLICATE KEY UPDATE
    tax_category = VALUES(tax_category),
    tax_dependent_count = VALUES(tax_dependent_count),
    paid_leave_remaining_days = VALUES(paid_leave_remaining_days),
    income_tax_calc_flag = TRUE,
    resident_tax_calc_flag = VALUES(resident_tax_calc_flag),
    resident_tax_monthly = 0,
    employment_insurance_flag = TRUE,
    social_insurance_flag = TRUE,
    health_insurance_flag = TRUE,
    pension_insurance_flag = TRUE,
    care_insurance_flag = VALUES(care_insurance_flag),
    daily_pay_flag = VALUES(daily_pay_flag),
    commute_allowance_monthly = VALUES(commute_allowance_monthly),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

-- 社会保険計算は従業員ごとの標準報酬月額を正本とする。
-- 8月検証用の総支給額に近い等級相当額を用意し、料率取込結果を確認できるようにする。
INSERT INTO employee_standard_remuneration (
    employee_id, effective_from, effective_to,
    health_standard_remuneration, pension_standard_remuneration,
    source_type, note,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    fixture.employee_id, '2026-08-01', NULL,
    CASE fixture.sort_order
        WHEN 1 THEN 260000 WHEN 2 THEN 290000
        WHEN 3 THEN 280000 WHEN 4 THEN 310000
        WHEN 5 THEN 300000 WHEN 6 THEN 300000
        WHEN 7 THEN 330000 ELSE 320000
    END,
    CASE fixture.sort_order
        WHEN 1 THEN 260000 WHEN 2 THEN 290000
        WHEN 3 THEN 280000 WHEN 4 THEN 310000
        WHEN 5 THEN 300000 WHEN 6 THEN 300000
        WHEN 7 THEN 330000 ELSE 320000
    END,
    'LOCAL_FIXTURE', '2026年8月Local税・社会保険検証用',
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tmp_august_employees fixture
ON DUPLICATE KEY UPDATE
    effective_to = NULL,
    health_standard_remuneration = VALUES(health_standard_remuneration),
    pension_standard_remuneration = VALUES(pension_standard_remuneration),
    source_type = VALUES(source_type),
    note = VALUES(note),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

-- 2026年8月は取込済みの2026年度料率・所得税表を使用する。
UPDATE payroll_calculation_period
SET income_tax_year = 2026,
    insurance_rate_year = 2026,
    child_care_support_required = TRUE,
    rounding_mode = 'HALF_UP',
    verified_flag = TRUE,
    verified_at = CURRENT_TIMESTAMP(6),
    verified_by = 'local-e2e',
    source_note = '2026年8月Local税・社会保険検証用',
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE tenant_id = @fixture_tenant_id
  AND target_month = @fixture_target_month;

INSERT INTO resident_tax_monthly (
    employee_id, fiscal_year, month, tax_amount,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    fixture.employee_id, 2026, 8, fixture.resident_tax,
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tmp_august_employees fixture
WHERE fixture.resident_tax > 0
ON DUPLICATE KEY UPDATE
    tax_amount = VALUES(tax_amount),
    tenant_id = VALUES(tenant_id),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

DROP TEMPORARY TABLE IF EXISTS tmp_august_dates;
CREATE TEMPORARY TABLE tmp_august_dates (
    work_date date primary key,
    day_order int not null,
    weekly_payment_date date not null
);

-- 2026-08-11 is excluded as a public holiday, giving twenty business days.
INSERT INTO tmp_august_dates VALUES
    ('2026-08-03',  1, '2026-08-07'),
    ('2026-08-04',  2, '2026-08-07'),
    ('2026-08-05',  3, '2026-08-07'),
    ('2026-08-06',  4, '2026-08-07'),
    ('2026-08-07',  5, '2026-08-07'),
    ('2026-08-10',  6, '2026-08-14'),
    ('2026-08-12',  7, '2026-08-14'),
    ('2026-08-13',  8, '2026-08-14'),
    ('2026-08-14',  9, '2026-08-14'),
    ('2026-08-17', 10, '2026-08-21'),
    ('2026-08-18', 11, '2026-08-21'),
    ('2026-08-19', 12, '2026-08-21'),
    ('2026-08-20', 13, '2026-08-21'),
    ('2026-08-21', 14, '2026-08-21'),
    ('2026-08-24', 15, '2026-08-28'),
    ('2026-08-25', 16, '2026-08-28'),
    ('2026-08-26', 17, '2026-08-28'),
    ('2026-08-27', 18, '2026-08-28'),
    ('2026-08-28', 19, '2026-08-28'),
    ('2026-08-31', 20, '2026-09-04');

SET @fixture_customer_id = (
    SELECT id FROM customers
    WHERE tenant_id = @fixture_tenant_id
      AND name = 'E2E 建設株式会社'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @fixture_site_id = (
    SELECT id FROM customer_sites
    WHERE tenant_id = @fixture_tenant_id
      AND customer_id = @fixture_customer_id
      AND name = 'E2E 丸の内建設現場'
      AND deleted_at IS NULL
    LIMIT 1
);
SET @fixture_billing_rate_id = (
    SELECT id FROM customer_site_billing_rates
    WHERE tenant_id = @fixture_tenant_id
      AND customer_site_id = @fixture_site_id
      AND job_code = 'GENERAL_WORK'
      AND site_role_code = 'GENERAL'
      AND deleted_at IS NULL
    LIMIT 1
);

-- Remove only this fixture's previous reports and child rows.
DELETE item
FROM daily_report_allowances item
JOIN daily_report report ON report.id = item.daily_report_id
JOIN tmp_august_employees fixture ON fixture.employee_id = report.employee_id
WHERE report.work_date BETWEEN '2026-08-01' AND '2026-08-31';

DELETE item
FROM daily_report_deductions item
JOIN daily_report report ON report.id = item.daily_report_id
JOIN tmp_august_employees fixture ON fixture.employee_id = report.employee_id
WHERE report.work_date BETWEEN '2026-08-01' AND '2026-08-31';

DELETE report
FROM daily_report report
JOIN tmp_august_employees fixture ON fixture.employee_id = report.employee_id
WHERE report.work_date BETWEEN '2026-08-01' AND '2026-08-31';

INSERT INTO daily_report (
    tenant_id, created_at, updated_at, deleted_at,
    employee_id, work_date, payment_date,
    customer_id, customer_site_id, customer_name, site_name,
    billing_rate_id, billing_unit,
    job_code, job_name, site_role_code, site_role_name,
    billing_base_unit_price, billing_overtime_unit_price,
    billing_night_unit_price, billing_holiday_unit_price,
    billing_commute_unit_price, work_description,
    start_time, end_time, break_minutes,
    work_hours, overtime_hours, night_work_hours, holiday_work_hours,
    holiday_premium_eligible,
    allowance_amount, deduction_amount,
    loan_repayment_amount, saving_amount, dormitory_charge_days,
    normal_pay_amount, overtime_pay_amount,
    night_pay_amount, holiday_pay_amount,
    estimated_gross_pay_amount, estimated_net_pay_amount,
    vehicle_used_flag, mileage, paid_leave_days,
    approval_status, approval_comment
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    employee.employee_id, date_row.work_date,
    CASE employee.payment_cycle
        WHEN 'DAILY' THEN date_row.work_date
        WHEN 'WEEKLY' THEN date_row.weekly_payment_date
        ELSE '2026-09-15'
    END,
    @fixture_customer_id, @fixture_site_id,
    'E2E 建設株式会社', 'E2E 丸の内建設現場',
    @fixture_billing_rate_id, 'DAILY',
    'GENERAL_WORK', '一般作業員', 'GENERAL', '一般',
    22000, 3438, 4125, 29700, 30,
    CONCAT('2026年8月月次締め検証 ', date_row.day_order),
    '08:00:00',
    IF(date_row.day_order IN (3, 13), '19:00:00', '17:00:00'),
    60,
    IF(employee.employee_code = 'E2E-AUG-MD-002' AND date_row.work_date = '2026-08-24', 0, 8),
    IF(date_row.day_order IN (3, 13), 2, 0),
    IF(date_row.work_date = '2026-08-14', 1, 0),
    0, FALSE,
    0, 0,
    IF(employee.employee_code = 'E2E-AUG-MD-002' AND date_row.work_date IN ('2026-08-07','2026-08-14','2026-08-21','2026-08-28'), 2000, 0),
    IF(employee.employee_code = 'E2E-AUG-MD-001' AND date_row.work_date IN ('2026-08-07','2026-08-14','2026-08-21','2026-08-28'), 500, 0),
    IF(employee.employee_code = 'E2E-AUG-D-001', 1, 0),
    CASE employee.salary_type
        WHEN 'MONTHLY' THEN ROUND(employee.monthly_salary / 20, 0)
        WHEN 'WEEKLY' THEN ROUND(employee.weekly_wage / 5, 0)
        ELSE employee.daily_wage
    END,
    IF(
        date_row.day_order IN (3, 13),
        ROUND((CASE employee.salary_type
            WHEN 'MONTHLY' THEN employee.monthly_salary / 20
            WHEN 'WEEKLY' THEN employee.weekly_wage / 5
            ELSE employee.daily_wage
        END) / 8 * 2 * 1.25, 0),
        0
    ),
    IF(
        date_row.work_date = '2026-08-14',
        ROUND((CASE employee.salary_type
            WHEN 'MONTHLY' THEN employee.monthly_salary / 20
            WHEN 'WEEKLY' THEN employee.weekly_wage / 5
            ELSE employee.daily_wage
        END) / 8 * 0.25, 0),
        0
    ),
    0,
    0, 0,
    MOD(employee.sort_order, 2) = 0,
    IF(MOD(employee.sort_order, 2) = 0, 16, 0),
    IF(employee.employee_code = 'E2E-AUG-MD-002' AND date_row.work_date = '2026-08-24', 1, 0),
    'APPROVED', '2026年8月Local月次締めfixture'
FROM tmp_august_employees employee
CROSS JOIN tmp_august_dates date_row;

-- Dynamic allowance: attendance-attitude allowance on each Friday close.
INSERT INTO daily_report_allowances (
    daily_report_id, allowance_master_id, allowance_code, allowance_name,
    amount, calculated_amount, manual_override_flag, override_reason,
    quantity, balance_unit,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    report.id, master.id, master.allowance_code, master.allowance_name,
    1000, 1000, FALSE, NULL, 1000, 'AMOUNT',
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM daily_report report
JOIN tmp_august_employees employee ON employee.employee_id = report.employee_id
JOIN allowance_masters master
  ON master.tenant_id = @fixture_tenant_id
 AND master.allowance_code = 'ATTENDANCE_ATTITUDE'
 AND master.deleted_at IS NULL
WHERE report.work_date IN ('2026-08-07','2026-08-14','2026-08-21','2026-08-28')
  AND report.deleted_at IS NULL;

-- Every report has a legal reserve estimate; selected employees also exercise
-- dormitory, mobile, Wi-Fi and short-term advance master-driven deductions.
INSERT INTO daily_report_deductions (
    daily_report_id, deduction_master_id, deduction_code, deduction_name,
    amount, calculated_amount, manual_override_flag, override_reason,
    quantity, balance_unit,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    report.id, master.id, master.deduction_code, master.deduction_name,
    300, 300, FALSE, NULL, 300, 'AMOUNT',
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM daily_report report
JOIN tmp_august_employees employee ON employee.employee_id = report.employee_id
JOIN deduction_masters master
  ON master.tenant_id = @fixture_tenant_id
 AND master.deduction_code = 'LEGAL_DEPOSIT'
 AND master.deleted_at IS NULL
WHERE report.work_date BETWEEN '2026-08-01' AND '2026-08-31'
  AND report.deleted_at IS NULL;

INSERT INTO daily_report_deductions (
    daily_report_id, deduction_master_id, deduction_code, deduction_name,
    amount, calculated_amount, manual_override_flag, override_reason,
    quantity, balance_unit,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    report.id, master.id, master.deduction_code, master.deduction_name,
    CASE master.deduction_code
        WHEN 'DORMITORY_FEE' THEN 1000
        WHEN 'MOBILE_RENTAL' THEN 5000
        WHEN 'WIFI_FEE' THEN 2500
        WHEN 'SHORT_TERM_ADVANCE' THEN 10000
    END,
    CASE master.deduction_code
        WHEN 'DORMITORY_FEE' THEN 1000
        WHEN 'MOBILE_RENTAL' THEN 5000
        WHEN 'WIFI_FEE' THEN 2500
        WHEN 'SHORT_TERM_ADVANCE' THEN 10000
    END,
    FALSE, NULL,
    CASE master.deduction_code
        WHEN 'DORMITORY_FEE' THEN 1000
        WHEN 'MOBILE_RENTAL' THEN 5000
        WHEN 'WIFI_FEE' THEN 2500
        WHEN 'SHORT_TERM_ADVANCE' THEN 10000
    END,
    'AMOUNT',
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM daily_report report
JOIN tmp_august_employees employee ON employee.employee_id = report.employee_id
JOIN deduction_masters master
  ON master.tenant_id = @fixture_tenant_id
 AND master.deduction_code IN ('DORMITORY_FEE','MOBILE_RENTAL','WIFI_FEE','SHORT_TERM_ADVANCE')
 AND master.deleted_at IS NULL
WHERE report.deleted_at IS NULL
  AND (
      (employee.employee_code = 'E2E-AUG-D-001'
       AND master.deduction_code = 'DORMITORY_FEE')
      OR
      (employee.employee_code = 'E2E-AUG-D-002'
       AND report.work_date = '2026-08-31'
       AND master.deduction_code IN ('MOBILE_RENTAL','WIFI_FEE'))
      OR
      (employee.employee_code = 'E2E-AUG-MD-001'
       AND report.work_date = '2026-08-31'
       AND master.deduction_code = 'SHORT_TERM_ADVANCE')
  );

-- Keep denormalized daily totals consistent with the dynamic detail rows.
UPDATE daily_report report
JOIN tmp_august_employees employee ON employee.employee_id = report.employee_id
SET report.allowance_amount = COALESCE((
        SELECT SUM(item.amount)
        FROM daily_report_allowances item
        WHERE item.daily_report_id = report.id
          AND item.deleted_at IS NULL
    ), 0),
    report.deduction_amount = COALESCE((
        SELECT SUM(item.amount)
        FROM daily_report_deductions item
        WHERE item.daily_report_id = report.id
          AND item.deleted_at IS NULL
    ), 0),
    report.estimated_gross_pay_amount =
        COALESCE(report.normal_pay_amount, 0)
        + COALESCE(report.overtime_pay_amount, 0)
        + COALESCE(report.night_pay_amount, 0)
        + COALESCE(report.holiday_pay_amount, 0)
        + COALESCE((
            SELECT SUM(item.amount)
            FROM daily_report_allowances item
            WHERE item.daily_report_id = report.id
              AND item.deleted_at IS NULL
          ), 0),
    report.estimated_net_pay_amount =
        COALESCE(report.normal_pay_amount, 0)
        + COALESCE(report.overtime_pay_amount, 0)
        + COALESCE(report.night_pay_amount, 0)
        + COALESCE(report.holiday_pay_amount, 0)
        + COALESCE((
            SELECT SUM(item.amount)
            FROM daily_report_allowances item
            WHERE item.daily_report_id = report.id
              AND item.deleted_at IS NULL
          ), 0)
        - COALESCE((
            SELECT SUM(item.amount)
            FROM daily_report_deductions item
            WHERE item.daily_report_id = report.id
              AND item.deleted_at IS NULL
          ), 0)
        - COALESCE(report.loan_repayment_amount, 0)
        - COALESCE(report.saving_amount, 0),
    report.updated_at = CURRENT_TIMESTAMP(6)
WHERE report.work_date BETWEEN '2026-08-01' AND '2026-08-31';

DELETE saving
FROM employee_saving saving
JOIN tmp_august_employees employee ON employee.employee_id = saving.employee_id;

INSERT INTO employee_saving (
    employee_id, percentage, min_salary_threshold, current_balance,
    active_flag, approval_status, approval_comment,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    employee_id, 5, 0, 20000,
    TRUE, 'APPROVED', '2026年8月Local月次締めfixture',
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tmp_august_employees
WHERE employee_code = 'E2E-AUG-MD-001';

DELETE loan
FROM employee_loan loan
JOIN tmp_august_employees employee ON employee.employee_id = loan.employee_id;

INSERT INTO employee_loan (
    employee_id, principal, monthly_repayment,
    loan_date, current_balance, repayment_start_date,
    active_flag, approval_status, approval_comment,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    employee_id, 100000, 8000,
    '2026-06-01', 60000, '2026-08-01',
    TRUE, 'APPROVED', '2026年8月Local月次締めfixture',
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tmp_august_employees
WHERE employee_code = 'E2E-AUG-MD-002';

SELECT COUNT(*) INTO @fixture_august_employee_count
FROM tmp_august_employees;
SELECT COUNT(*) INTO @fixture_august_report_count
FROM daily_report report
JOIN tmp_august_employees employee ON employee.employee_id = report.employee_id
WHERE report.work_date BETWEEN '2026-08-01' AND '2026-08-31'
  AND report.deleted_at IS NULL;
SELECT COUNT(*) INTO @fixture_august_allowance_count
FROM daily_report_allowances item
JOIN daily_report report ON report.id = item.daily_report_id
JOIN tmp_august_employees employee ON employee.employee_id = report.employee_id
WHERE report.work_date BETWEEN '2026-08-01' AND '2026-08-31'
  AND item.deleted_at IS NULL;
SELECT COUNT(*) INTO @fixture_august_deduction_count
FROM daily_report_deductions item
JOIN daily_report report ON report.id = item.daily_report_id
JOIN tmp_august_employees employee ON employee.employee_id = report.employee_id
WHERE report.work_date BETWEEN '2026-08-01' AND '2026-08-31'
  AND item.deleted_at IS NULL;

SELECT
    @fixture_august_employee_count AS fixture_employees,
    @fixture_august_report_count AS fixture_daily_reports,
    @fixture_august_allowance_count AS fixture_allowance_items,
    @fixture_august_deduction_count AS fixture_deduction_items;

DROP TEMPORARY TABLE IF EXISTS tmp_august_dates;
DROP TEMPORARY TABLE IF EXISTS tmp_august_employees;
