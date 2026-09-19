-- ProjectAdminSystem V1
-- ローカルDocker専用：日報・日次管理・日次給与明細の総合確認fixture
--
-- 本番環境では適用しない。
-- E2E-DAILY-* / E2E-WEEKLY-* / E2E-MONTHLY-* の従業員を正本として再作成するため、
-- Docker再起動時にも同じ件数・金額で検証できる。

SET @fixture_tenant_id = 'default';
SET @fixture_payment_date = '2026-09-05';

DROP TEMPORARY TABLE IF EXISTS tmp_daily_customers;
CREATE TEMPORARY TABLE tmp_daily_customers (
    customer_key varchar(20) primary key,
    customer_id bigint null,
    customer_name varchar(255) not null,
    short_name varchar(255) not null,
    contract_flag varchar(20) not null,
    invoice_type varchar(20) not null,
    closing_day_type varchar(30) not null,
    closing_day_value int null
);

INSERT INTO tmp_daily_customers VALUES
    ('BUILD', NULL, 'E2E 建設株式会社', 'E2E建設', 'ACTIVE', 'PATTERN_1', 'END_OF_MONTH', NULL),
    ('CIVIL', NULL, 'E2E 土木株式会社', 'E2E土木', 'ACTIVE', 'PATTERN_2', 'DAY_OF_MONTH', 20),
    ('EQUIP', NULL, 'E2E 設備株式会社', 'E2E設備', 'ACTIVE', 'PATTERN_3', 'DAY_OF_MONTH', 25),
    ('INACTIVE', NULL, 'E2E 契約終了株式会社', 'E2E終了', 'INACTIVE', 'PATTERN_1', 'END_OF_MONTH', NULL);

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
    fixture.customer_name, CONCAT('いーつーいー', fixture.customer_key), fixture.short_name,
    '100-0001', CONCAT('東京都千代田区テスト ', fixture.customer_key),
    CONCAT(fixture.short_name, ' 代表'), '03-0000-0000',
    '建設', fixture.contract_flag, fixture.invoice_type,
    fixture.closing_day_type, fixture.closing_day_value, 0,
    'DAY_OF_MONTH', 25, 1
FROM tmp_daily_customers fixture
WHERE NOT EXISTS (
    SELECT 1
    FROM customers customer
    WHERE customer.tenant_id = @fixture_tenant_id
      AND customer.name = fixture.customer_name
);

UPDATE customers customer
JOIN tmp_daily_customers fixture
  ON customer.tenant_id = @fixture_tenant_id
 AND customer.name = fixture.customer_name
SET customer.deleted_at = NULL,
    customer.short_name = fixture.short_name,
    customer.contract_flag = fixture.contract_flag,
    customer.invoice_type = fixture.invoice_type,
    customer.closing_day_type = fixture.closing_day_type,
    customer.closing_day_value = fixture.closing_day_value,
    customer.updated_at = CURRENT_TIMESTAMP(6);

UPDATE tmp_daily_customers fixture
JOIN customers customer
  ON customer.tenant_id = @fixture_tenant_id
 AND customer.name = fixture.customer_name
SET fixture.customer_id = customer.id;

DROP TEMPORARY TABLE IF EXISTS tmp_daily_sites;
CREATE TEMPORARY TABLE tmp_daily_sites (
    site_key varchar(20) primary key,
    site_id bigint null,
    customer_key varchar(20) not null,
    site_name varchar(255) not null,
    distance_km int not null,
    job_code varchar(100) not null,
    job_name varchar(200) not null,
    base_unit_price decimal(15,2) not null
);

INSERT INTO tmp_daily_sites VALUES
    ('BUILD_A', NULL, 'BUILD', 'E2E 丸の内建設現場', 8, 'GENERAL_WORK', '一般作業員', 22000),
    ('BUILD_B', NULL, 'BUILD', 'E2E 品川建設現場', 18, 'FORM_WORK', '型枠作業員', 24000),
    ('CIVIL_A', NULL, 'CIVIL', 'E2E 横浜土木現場', 32, 'CIVIL_WORK', '土木作業員', 23000),
    ('CIVIL_B', NULL, 'CIVIL', 'E2E 川崎道路現場', 27, 'ROAD_WORK', '道路作業員', 23500),
    ('EQUIP_A', NULL, 'EQUIP', 'E2E 新宿設備現場', 12, 'EQUIPMENT_WORK', '設備作業員', 25000),
    ('EQUIP_B', NULL, 'EQUIP', 'E2E 渋谷設備現場', 10, 'ELECTRICAL_WORK', '電気作業員', 26000);

INSERT INTO customer_sites (
    tenant_id, created_at, updated_at, deleted_at,
    customer_id, name,
    contact_person_name, contact_person_phone,
    contact_person_email, distance_from_company_km
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    customer.customer_id, site.site_name,
    CONCAT(site.site_name, ' 責任者'), '090-0000-0000',
    CONCAT(LOWER(site.site_key), '@example.invalid'), site.distance_km
FROM tmp_daily_sites site
JOIN tmp_daily_customers customer
  ON customer.customer_key = site.customer_key
WHERE NOT EXISTS (
    SELECT 1
    FROM customer_sites current_site
    WHERE current_site.tenant_id = @fixture_tenant_id
      AND current_site.customer_id = customer.customer_id
      AND current_site.name = site.site_name
);

UPDATE customer_sites current_site
JOIN tmp_daily_customers customer
  ON current_site.customer_id = customer.customer_id
JOIN tmp_daily_sites site
  ON site.customer_key = customer.customer_key
 AND current_site.name = site.site_name
SET current_site.deleted_at = NULL,
    current_site.distance_from_company_km = site.distance_km,
    current_site.updated_at = CURRENT_TIMESTAMP(6);

UPDATE tmp_daily_sites site
JOIN tmp_daily_customers customer
  ON customer.customer_key = site.customer_key
JOIN customer_sites current_site
  ON current_site.tenant_id = @fixture_tenant_id
 AND current_site.customer_id = customer.customer_id
 AND current_site.name = site.site_name
SET site.site_id = current_site.id;

INSERT INTO customer_site_billing_rates (
    tenant_id, created_at, updated_at, deleted_at,
    customer_site_id, job_code, job_name,
    site_role_code, site_role_name, billing_unit,
    base_unit_price, overtime_unit_price, night_unit_price,
    holiday_unit_price, commute_unit_price,
    effective_from, effective_to, display_order, active_flag, note
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    site.site_id, site.job_code, site.job_name,
    'GENERAL', '一般', 'DAILY',
    site.base_unit_price,
    ROUND(site.base_unit_price / 8 * 1.25, 0),
    ROUND(site.base_unit_price / 8 * 1.50, 0),
    ROUND(site.base_unit_price * 1.35, 0),
    30,
    '2026-04-01', NULL, 10, TRUE,
    'ローカル日次業務確認用'
FROM tmp_daily_sites site
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name),
    billing_unit = VALUES(billing_unit),
    base_unit_price = VALUES(base_unit_price),
    overtime_unit_price = VALUES(overtime_unit_price),
    night_unit_price = VALUES(night_unit_price),
    holiday_unit_price = VALUES(holiday_unit_price),
    commute_unit_price = VALUES(commute_unit_price),
    effective_to = NULL,
    active_flag = TRUE,
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO customer_employees (
    tenant_id, created_at, updated_at, deleted_at,
    customer_id, name, furigana_name, position,
    phone, email, invoice_to_flag, invoice_cc_flag
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    customer.customer_id, CONCAT(customer.short_name, ' 請求担当'),
    'イーツーイー セイキュウタントウ', '経理担当',
    '090-1111-2222', CONCAT(LOWER(customer.customer_key), '.to@example.invalid'),
    TRUE, FALSE
FROM tmp_daily_customers customer
WHERE customer.contract_flag = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM customer_employees employee
      WHERE employee.tenant_id = @fixture_tenant_id
        AND employee.customer_id = customer.customer_id
        AND employee.email = CONCAT(LOWER(customer.customer_key), '.to@example.invalid')
  );

INSERT INTO customer_employees (
    tenant_id, created_at, updated_at, deleted_at,
    customer_id, name, furigana_name, position,
    phone, email, invoice_to_flag, invoice_cc_flag
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    customer.customer_id, CONCAT(customer.short_name, ' 現場担当'),
    'イーツーイー ゲンバタントウ', '現場担当',
    '090-3333-4444', CONCAT(LOWER(customer.customer_key), '.cc@example.invalid'),
    FALSE, TRUE
FROM tmp_daily_customers customer
WHERE customer.contract_flag = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM customer_employees employee
      WHERE employee.tenant_id = @fixture_tenant_id
        AND employee.customer_id = customer.customer_id
        AND employee.email = CONCAT(LOWER(customer.customer_key), '.cc@example.invalid')
  );

DROP TEMPORARY TABLE IF EXISTS tmp_daily_employees;
CREATE TEMPORARY TABLE tmp_daily_employees (
    employee_code varchar(100) primary key,
    employee_id bigint null,
    employee_name varchar(200) not null,
    employee_name_kana varchar(200) not null,
    payment_cycle varchar(20) not null,
    salary_type varchar(20) not null,
    daily_wage decimal(12,2) not null,
    monthly_salary decimal(12,2) not null,
    site_key varchar(20) not null,
    sort_order int not null
);

INSERT INTO tmp_daily_employees VALUES
    ('E2E-DAILY-001', NULL, '日次検証 青木 一郎', 'ニチジケンショウ アオキ イチロウ', 'DAILY', 'DAILY', 12000, 0, 'BUILD_A', 1),
    ('E2E-DAILY-002', NULL, '日次検証 伊藤 二郎', 'ニチジケンショウ イトウ ジロウ', 'DAILY', 'DAILY', 12500, 0, 'BUILD_B', 2),
    ('E2E-DAILY-003', NULL, '日次検証 上田 三郎', 'ニチジケンショウ ウエダ サブロウ', 'DAILY', 'DAILY', 13000, 0, 'CIVIL_A', 3),
    ('E2E-DAILY-004', NULL, '日次検証 江藤 四郎', 'ニチジケンショウ エトウ シロウ', 'DAILY', 'DAILY', 13500, 0, 'CIVIL_B', 4),
    ('E2E-DAILY-005', NULL, '日次検証 大野 五郎', 'ニチジケンショウ オオノ ゴロウ', 'DAILY', 'DAILY', 14000, 0, 'EQUIP_A', 5),
    ('E2E-DAILY-006', NULL, '日次検証 加藤 六郎', 'ニチジケンショウ カトウ ロクロウ', 'DAILY', 'DAILY', 14500, 0, 'EQUIP_B', 6),
    ('E2E-DAILY-007', NULL, '日次検証 木村 七郎', 'ニチジケンショウ キムラ シチロウ', 'DAILY', 'DAILY', 15000, 0, 'BUILD_A', 7),
    ('E2E-DAILY-008', NULL, '日次検証 佐藤 八郎', 'ニチジケンショウ サトウ ハチロウ', 'DAILY', 'DAILY', 15500, 0, 'CIVIL_A', 8),
    ('E2E-WEEKLY-051', NULL, '週次検証 山本 一郎', 'シュウジケンショウ ヤマモト イチロウ', 'WEEKLY', 'DAILY', 13000, 0, 'BUILD_A', 51),
    ('E2E-WEEKLY-052', NULL, '週次検証 吉田 二郎', 'シュウジケンショウ ヨシダ ジロウ', 'WEEKLY', 'DAILY', 13500, 0, 'CIVIL_B', 52),
    ('E2E-MONTHLY-101', NULL, '月次検証 高橋 一郎', 'ゲツジケンショウ タカハシ イチロウ', 'MONTHLY', 'MONTHLY', 0, 300000, 'EQUIP_A', 101),
    ('E2E-MONTHLY-102', NULL, '月次検証 中村 二郎', 'ゲツジケンショウ ナカムラ ジロウ', 'MONTHLY', 'MONTHLY', 0, 320000, 'BUILD_B', 102),
    ('E2E-MONTHLY-DAILY-103', NULL, '月次支払日給検証 鈴木 三郎', 'ゲツジシハライニッキュウケンショウ スズキ サブロウ', 'MONTHLY', 'DAILY', 14500, 0, 'CIVIL_A', 103);

INSERT INTO employee (
    tenant_id, created_at, updated_at, deleted_at,
    employee_code, employee_name, employee_name_kana,
    birth_date, hire_date, employment_type, employment_status,
    phone, email, postal_code, address,
    dormitory_flag, dormitory_type, active_flag
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    fixture.employee_code, fixture.employee_name, fixture.employee_name_kana,
    DATE_ADD('1985-01-01', INTERVAL fixture.sort_order DAY), '2026-04-01',
    'FULL_TIME', 'ACTIVE', '090-5555-0000',
    CONCAT(LOWER(fixture.employee_code), '@example.invalid'),
    CONCAT('100-', LPAD(fixture.sort_order, 4, '0')),
    CONCAT('東京都千代田区日次検証', fixture.sort_order, '-1'),
    MOD(fixture.sort_order, 3) = 0,
    IF(MOD(fixture.sort_order, 3) = 0, 'SHARED_ROOM', NULL), TRUE
FROM tmp_daily_employees fixture
ON DUPLICATE KEY UPDATE
    employee_name = VALUES(employee_name),
    employee_name_kana = VALUES(employee_name_kana),
    postal_code = VALUES(postal_code),
    address = VALUES(address),
    employment_status = 'ACTIVE',
    active_flag = TRUE,
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

UPDATE tmp_daily_employees fixture
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
    fixture.employee_id, '2026-04-01', NULL,
    FALSE, fixture.salary_type, fixture.payment_cycle,
    fixture.monthly_salary, 0, fixture.daily_wage, 0,
    40, 'ローカル日次・月次総合確認用'
FROM tmp_daily_employees fixture
ON DUPLICATE KEY UPDATE
    contract_start_date = VALUES(contract_start_date),
    contract_end_date = NULL,
    salary_type = VALUES(salary_type),
    payment_cycle = VALUES(payment_cycle),
    monthly_salary = VALUES(monthly_salary),
    daily_wage = VALUES(daily_wage),
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
    TRUE, TRUE, 8000 + MOD(fixture.sort_order, 4) * 1000,
    TRUE, TRUE, TRUE, TRUE,
    FALSE, fixture.payment_cycle = 'DAILY', 5000
FROM tmp_daily_employees fixture
ON DUPLICATE KEY UPDATE
    tax_category = VALUES(tax_category),
    tax_dependent_count = VALUES(tax_dependent_count),
    resident_tax_monthly = VALUES(resident_tax_monthly),
    daily_pay_flag = VALUES(daily_pay_flag),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

DROP TEMPORARY TABLE IF EXISTS tmp_daily_dates;
CREATE TEMPORARY TABLE tmp_daily_dates (
    work_date date primary key,
    day_order int not null,
    start_time time not null,
    end_time time not null,
    work_hours decimal(5,2) not null,
    overtime_hours decimal(5,2) not null,
    night_hours decimal(5,2) not null
);

INSERT INTO tmp_daily_dates VALUES
    ('2026-09-01', 1, '08:00:00', '17:00:00', 8, 0, 0),
    ('2026-09-02', 2, '08:00:00', '19:00:00', 8, 2, 0),
    ('2026-09-03', 3, '09:00:00', '18:00:00', 8, 0, 0),
    ('2026-09-04', 4, '08:00:00', '22:00:00', 8, 5, 1),
    ('2026-09-05', 5, '08:00:00', '17:00:00', 8, 0, 0);

-- 再適用時に前回fixtureの日報明細を先に削除する。
DELETE deduction_item
FROM daily_report_deductions deduction_item
JOIN daily_report report
  ON report.id = deduction_item.daily_report_id
JOIN tmp_daily_employees fixture
  ON fixture.employee_id = report.employee_id;

DELETE allowance_item
FROM daily_report_allowances allowance_item
JOIN daily_report report
  ON report.id = allowance_item.daily_report_id
JOIN tmp_daily_employees fixture
  ON fixture.employee_id = report.employee_id;

DELETE report
FROM daily_report report
JOIN tmp_daily_employees fixture
  ON fixture.employee_id = report.employee_id;

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
    IF(
        employee.payment_cycle = 'DAILY',
        date_row.work_date,
        @fixture_payment_date
    ),
    customer.customer_id, site.site_id, customer.customer_name, site.site_name,
    rate.id, 'DAILY', site.job_code, site.job_name, 'GENERAL', '一般',
    site.base_unit_price,
    ROUND(site.base_unit_price / 8 * 1.25, 0),
    ROUND(site.base_unit_price / 8 * 1.50, 0),
    ROUND(site.base_unit_price * 1.35, 0),
    30, CONCAT('日次検証作業 ', date_row.day_order),
    date_row.start_time, date_row.end_time, 60,
    date_row.work_hours, date_row.overtime_hours, date_row.night_hours, 0,
    FALSE,
    IF(date_row.day_order = 3, 1000, 0),
    500 + MOD(employee.sort_order, 4) * 100,
    IF(date_row.day_order = 5, 1000, 0),
    IF(date_row.day_order = 4, 500, 0),
    IF(MOD(employee.sort_order, 3) = 0, 1, 0),
    IF(employee.salary_type = 'DAILY', employee.daily_wage, ROUND(employee.monthly_salary / 20, 0)),
    ROUND(date_row.overtime_hours * IF(employee.daily_wage > 0, employee.daily_wage, employee.monthly_salary / 20) / 8 * 1.25, 0),
    ROUND(date_row.night_hours * IF(employee.daily_wage > 0, employee.daily_wage, employee.monthly_salary / 20) / 8 * 0.25, 0),
    0,
    IF(employee.salary_type = 'DAILY', employee.daily_wage, ROUND(employee.monthly_salary / 20, 0))
      + IF(date_row.day_order = 3, 1000, 0)
      + ROUND(date_row.overtime_hours * IF(employee.daily_wage > 0, employee.daily_wage, employee.monthly_salary / 20) / 8 * 1.25, 0)
      + ROUND(date_row.night_hours * IF(employee.daily_wage > 0, employee.daily_wage, employee.monthly_salary / 20) / 8 * 0.25, 0),
    IF(employee.salary_type = 'DAILY', employee.daily_wage, ROUND(employee.monthly_salary / 20, 0))
      + IF(date_row.day_order = 3, 1000, 0)
      + ROUND(date_row.overtime_hours * IF(employee.daily_wage > 0, employee.daily_wage, employee.monthly_salary / 20) / 8 * 1.25, 0)
      + ROUND(date_row.night_hours * IF(employee.daily_wage > 0, employee.daily_wage, employee.monthly_salary / 20) / 8 * 0.25, 0)
      - (500 + MOD(employee.sort_order, 4) * 100)
      - IF(date_row.day_order = 5, 1000, 0)
      - IF(date_row.day_order = 4, 500, 0),
    MOD(employee.sort_order, 2) = 0,
    IF(MOD(employee.sort_order, 2) = 0, site.distance_km * 2, 0),
    0,
    'APPROVED', 'ローカルfixture自動承認'
FROM tmp_daily_employees employee
JOIN tmp_daily_sites site
  ON site.site_key = employee.site_key
JOIN tmp_daily_customers customer
  ON customer.customer_key = site.customer_key
JOIN customer_site_billing_rates rate
  ON rate.tenant_id = @fixture_tenant_id
 AND rate.customer_site_id = site.site_id
 AND rate.job_code = site.job_code
 AND rate.site_role_code = 'GENERAL'
 AND rate.effective_from = '2026-04-01'
 AND rate.deleted_at IS NULL
CROSS JOIN tmp_daily_dates date_row;

-- E2E-DAILY-001を、日次給与明細の控除・残高確認用従業員とする。
-- 携帯は請求明細で残高を発生させる。Wi-Fiは残高を持たず日報へ手入力する。
DELETE enrollment
FROM employee_payroll_item_enrollment enrollment
JOIN tmp_daily_employees employee
  ON employee.employee_id = enrollment.employee_id
JOIN payroll_item_balance_policy policy
  ON policy.id = enrollment.balance_policy_id
WHERE employee.employee_code = 'E2E-DAILY-001'
  AND policy.target_type = 'DEDUCTION'
  AND policy.target_code IN ('DORMITORY_FEE', 'MOBILE_RENTAL', 'WIFI_FEE');

INSERT INTO employee_payroll_item_enrollment (
    employee_id, balance_policy_id, effective_from, effective_to, settings_json,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT employee.employee_id, policy.id, '2026-09-01', NULL,
       CASE policy.target_code
           WHEN 'DORMITORY_FEE' THEN JSON_OBJECT(
               'dormitoryType', 'SHARED_ROOM',
               'dormitoryDailyAmount', '1500',
               'inputSource', 'DAILY_REPORT'
           )
           ELSE JSON_OBJECT()
       END,
       @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tmp_daily_employees employee
JOIN payroll_item_balance_policy policy
  ON policy.tenant_id = @fixture_tenant_id
 AND policy.target_type = 'DEDUCTION'
 AND policy.target_code IN ('DORMITORY_FEE', 'MOBILE_RENTAL', 'WIFI_FEE')
 AND policy.active_flag = TRUE
 AND policy.deleted_at IS NULL
WHERE employee.employee_code = 'E2E-DAILY-001';

DELETE transaction_item
FROM employee_payroll_item_transaction transaction_item
JOIN tmp_daily_employees employee
  ON employee.employee_id = transaction_item.employee_id
WHERE employee.employee_code = 'E2E-DAILY-001'
  AND transaction_item.source_reference IN (
      'E2E-MOBILE-202609-BILL',
      'E2E-WIFI-202609-BILL'
  );

INSERT INTO employee_payroll_item_transaction (
    employee_id, target_type, target_master_id,
    target_code, target_name, target_month,
    transaction_date, amount, quantity,
    transaction_purpose, balance_effect,
    source_type, source_reference, status, note,
    lock_version, tenant_id, created_at, updated_at, deleted_at
)
SELECT employee.employee_id, 'DEDUCTION', deduction.id,
       deduction.deduction_code, deduction.deduction_name, '2026-09-01',
       '2026-09-01',
       5000,
       5000,
       'BALANCE_ACCRUAL', 'CREDIT', 'MANUAL',
       'E2E-MOBILE-202609-BILL',
       'CONFIRMED', 'ローカル日次給与明細確認用の確定請求明細',
       0, @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tmp_daily_employees employee
JOIN deduction_masters deduction
  ON deduction.tenant_id = @fixture_tenant_id
 AND deduction.deduction_code = 'MOBILE_RENTAL'
 AND deduction.deleted_at IS NULL
WHERE employee.employee_code = 'E2E-DAILY-001';

INSERT INTO daily_report_deductions (
    daily_report_id, deduction_master_id, deduction_code, deduction_name,
    amount, calculated_amount, manual_override_flag, override_reason,
    quantity, balance_unit,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT report.id, deduction.id, deduction.deduction_code, deduction.deduction_name,
       CASE deduction.deduction_code
           WHEN 'DORMITORY_FEE' THEN 1500
           WHEN 'MOBILE_RENTAL' THEN 1000
           WHEN 'WIFI_FEE' THEN 1000
           WHEN 'LEGAL_DEPOSIT' THEN 700
       END,
       CASE deduction.deduction_code
           WHEN 'DORMITORY_FEE' THEN 1500
           WHEN 'MOBILE_RENTAL' THEN 1000
           WHEN 'WIFI_FEE' THEN 1000
           WHEN 'LEGAL_DEPOSIT' THEN 700
       END,
       FALSE, NULL,
       CASE deduction.deduction_code
           WHEN 'DORMITORY_FEE' THEN 1500
           WHEN 'MOBILE_RENTAL' THEN 1000
           WHEN 'WIFI_FEE' THEN 1000
           WHEN 'LEGAL_DEPOSIT' THEN 700
       END,
       'AMOUNT',
       @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM daily_report report
JOIN tmp_daily_employees employee
  ON employee.employee_id = report.employee_id
JOIN deduction_masters deduction
  ON deduction.tenant_id = @fixture_tenant_id
 AND deduction.deduction_code IN (
     'DORMITORY_FEE', 'MOBILE_RENTAL', 'WIFI_FEE', 'LEGAL_DEPOSIT'
 )
 AND deduction.deleted_at IS NULL
WHERE employee.employee_code = 'E2E-DAILY-001'
  AND report.work_date = '2026-09-05'
  AND report.deleted_at IS NULL;

-- 貯蓄も当日の支払額から控除される。日報合計と明細実績を同じ金額へ揃える。
UPDATE daily_report report
JOIN tmp_daily_employees employee
  ON employee.employee_id = report.employee_id
SET report.deduction_amount = 4200,
    report.saving_amount = 800,
    report.estimated_net_pay_amount =
        report.estimated_gross_pay_amount
        - 4200
        - COALESCE(report.loan_repayment_amount, 0)
        - 800,
    report.updated_at = CURRENT_TIMESTAMP(6)
WHERE employee.employee_code = 'E2E-DAILY-001'
  AND report.work_date = '2026-09-05'
  AND report.deleted_at IS NULL;

DELETE payment
FROM daily_payments payment
JOIN tmp_daily_employees fixture
  ON fixture.employee_id = payment.employee_id;

INSERT INTO daily_payments (
    tenant_id, created_at, updated_at, deleted_at,
    payment_date, employee_id, employee_code, employee_name,
    planned_amount, actual_amount, status, paid_at, note
)
SELECT
    @fixture_tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL,
    @fixture_payment_date, employee.employee_id,
    employee.employee_code, employee.employee_name,
    SUM(report.estimated_net_pay_amount),
    SUM(report.estimated_net_pay_amount),
    'PAID', CURRENT_TIMESTAMP(6),
    'ローカル日次給与明細確認用の確定支払'
FROM tmp_daily_employees employee
JOIN daily_report report
  ON report.employee_id = employee.employee_id
 AND report.payment_date = @fixture_payment_date
 AND report.deleted_at IS NULL
WHERE employee.payment_cycle = 'DAILY'
GROUP BY employee.employee_id, employee.employee_code,
         employee.employee_name, employee.sort_order;

SELECT COUNT(*) INTO @fixture_customer_count FROM tmp_daily_customers;
SELECT COUNT(*) INTO @fixture_site_count FROM tmp_daily_sites;
SELECT COUNT(*) INTO @fixture_employee_count FROM tmp_daily_employees;
SELECT COUNT(*) INTO @fixture_daily_report_count
FROM daily_report report
JOIN tmp_daily_employees employee
  ON employee.employee_id = report.employee_id;
SELECT COUNT(*) INTO @fixture_daily_payment_count
FROM daily_payments payment
JOIN tmp_daily_employees employee
  ON employee.employee_id = payment.employee_id;

SELECT
    @fixture_customer_count AS fixture_customers,
    @fixture_site_count AS fixture_sites,
    @fixture_employee_count AS fixture_employees,
    @fixture_daily_report_count AS fixture_daily_reports,
    @fixture_daily_payment_count AS fixture_daily_payments;

DROP TEMPORARY TABLE IF EXISTS tmp_daily_dates;
DROP TEMPORARY TABLE IF EXISTS tmp_daily_employees;
DROP TEMPORARY TABLE IF EXISTS tmp_daily_sites;
DROP TEMPORARY TABLE IF EXISTS tmp_daily_customers;
