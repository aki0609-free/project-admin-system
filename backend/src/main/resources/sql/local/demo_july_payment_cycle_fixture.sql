-- ProjectAdminSystem V1
-- ローカルDocker専用：2026年7月 支払周期（日給者）確認fixture
--
-- 対象
--   E2E-DAILY-001         : 日給・日払い（翌日。日曜日だけ後ろ倒し）
--   E2E-WEEKLY-051        : 日給・週払い（翌週月曜）
--   E2E-MONTHLY-DAILY-103 : 日給・月払い（翌月15日）
--
-- 7月20日（祝日も通常日扱い）と土曜日を含め、画面初期値と帳票集計を確認できる。
-- 本番環境では適用しない。

SET @fixture_tenant_id = 'default';

DROP TEMPORARY TABLE IF EXISTS tmp_july_cycle_dates;
CREATE TEMPORARY TABLE tmp_july_cycle_dates (
    work_date date primary key,
    daily_payment_date date not null,
    weekly_payment_date date not null,
    day_order int not null
);

INSERT INTO tmp_july_cycle_dates VALUES
    ('2026-07-01', '2026-07-02', '2026-07-06',  1),
    ('2026-07-02', '2026-07-03', '2026-07-06',  2),
    ('2026-07-03', '2026-07-04', '2026-07-06',  3),
    ('2026-07-04', '2026-07-06', '2026-07-06',  4),
    ('2026-07-06', '2026-07-07', '2026-07-13',  5),
    ('2026-07-10', '2026-07-11', '2026-07-13',  6),
    ('2026-07-11', '2026-07-13', '2026-07-13',  7),
    ('2026-07-13', '2026-07-14', '2026-07-20',  8),
    ('2026-07-17', '2026-07-18', '2026-07-20',  9),
    ('2026-07-18', '2026-07-20', '2026-07-20', 10),
    ('2026-07-21', '2026-07-22', '2026-07-27', 11),
    ('2026-07-25', '2026-07-27', '2026-07-27', 12),
    ('2026-07-27', '2026-07-28', '2026-08-03', 13),
    ('2026-07-31', '2026-08-01', '2026-08-03', 14);

DROP TEMPORARY TABLE IF EXISTS tmp_july_cycle_employees;
CREATE TEMPORARY TABLE tmp_july_cycle_employees AS
SELECT
    employee.id AS employee_id,
    employee.employee_code,
    employee.employee_name,
    contract.payment_cycle,
    contract.daily_wage
FROM employee
JOIN employee_contract contract
  ON contract.employee_id = employee.id
 AND contract.tenant_id = @fixture_tenant_id
 AND contract.deleted_at IS NULL
 AND contract.contract_start_date <= '2026-07-31'
 AND (contract.contract_end_date IS NULL OR contract.contract_end_date >= '2026-07-01')
WHERE employee.tenant_id = @fixture_tenant_id
  AND employee.deleted_at IS NULL
  AND employee.employee_code IN (
      'E2E-DAILY-001',
      'E2E-WEEKLY-051',
      'E2E-MONTHLY-DAILY-103'
  );

-- 単体で再適用しても、7月のfixtureだけを安全に作り直せるようにする。
DELETE deduction_item
FROM daily_report_deductions deduction_item
JOIN daily_report report
  ON report.id = deduction_item.daily_report_id
JOIN tmp_july_cycle_employees employee
  ON employee.employee_id = report.employee_id
WHERE report.work_date BETWEEN '2026-07-01' AND '2026-07-31';

DELETE allowance_item
FROM daily_report_allowances allowance_item
JOIN daily_report report
  ON report.id = allowance_item.daily_report_id
JOIN tmp_july_cycle_employees employee
  ON employee.employee_id = report.employee_id
WHERE report.work_date BETWEEN '2026-07-01' AND '2026-07-31';

DELETE report
FROM daily_report report
JOIN tmp_july_cycle_employees employee
  ON employee.employee_id = report.employee_id
WHERE report.work_date BETWEEN '2026-07-01' AND '2026-07-31';

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
        WHEN 'DAILY' THEN date_row.daily_payment_date
        WHEN 'WEEKLY' THEN date_row.weekly_payment_date
        WHEN 'MONTHLY' THEN '2026-08-15'
    END,
    customer.id, site.id, customer.name, site.name,
    rate.id, rate.billing_unit,
    rate.job_code, rate.job_name, rate.site_role_code, rate.site_role_name,
    rate.base_unit_price, rate.overtime_unit_price,
    rate.night_unit_price, rate.holiday_unit_price,
    rate.commute_unit_price,
    CONCAT('2026年7月支払周期検証 ', date_row.day_order),
    '08:00:00',
    IF(MOD(date_row.day_order, 4) = 0, '19:00:00', '17:00:00'),
    60,
    8,
    IF(MOD(date_row.day_order, 4) = 0, 2, 0),
    0,
    0,
    FALSE,
    IF(MOD(date_row.day_order, 5) = 0, 1000, 0),
    600,
    0,
    0,
    0,
    employee.daily_wage,
    IF(
        MOD(date_row.day_order, 4) = 0,
        ROUND(employee.daily_wage / 8 * 1.25 * 2, 0),
        0
    ),
    0,
    0,
    employee.daily_wage
      + IF(MOD(date_row.day_order, 5) = 0, 1000, 0)
      + IF(
          MOD(date_row.day_order, 4) = 0,
          ROUND(employee.daily_wage / 8 * 1.25 * 2, 0),
          0
        ),
    employee.daily_wage
      + IF(MOD(date_row.day_order, 5) = 0, 1000, 0)
      + IF(
          MOD(date_row.day_order, 4) = 0,
          ROUND(employee.daily_wage / 8 * 1.25 * 2, 0),
          0
        )
      - 600,
    FALSE,
    0,
    0,
    'APPROVED',
    'ローカル2026年7月支払周期fixture'
FROM tmp_july_cycle_employees employee
JOIN customers customer
  ON customer.tenant_id = @fixture_tenant_id
 AND customer.name = 'E2E 建設株式会社'
 AND customer.deleted_at IS NULL
JOIN customer_sites site
  ON site.tenant_id = @fixture_tenant_id
 AND site.customer_id = customer.id
 AND site.name = 'E2E 丸の内建設現場'
 AND site.deleted_at IS NULL
JOIN customer_site_billing_rates rate
  ON rate.tenant_id = @fixture_tenant_id
 AND rate.customer_site_id = site.id
 AND rate.job_code = 'GENERAL_WORK'
 AND rate.site_role_code = 'GENERAL'
 AND rate.effective_from <= '2026-07-31'
 AND (rate.effective_to IS NULL OR rate.effective_to >= '2026-07-01')
 AND rate.deleted_at IS NULL
CROSS JOIN tmp_july_cycle_dates date_row;

SELECT
    employee.employee_code,
    employee.payment_cycle,
    COUNT(report.id) AS july_daily_reports,
    MIN(report.payment_date) AS first_payment_date,
    MAX(report.payment_date) AS last_payment_date
FROM tmp_july_cycle_employees employee
JOIN daily_report report
  ON report.employee_id = employee.employee_id
 AND report.work_date BETWEEN '2026-07-01' AND '2026-07-31'
 AND report.deleted_at IS NULL
GROUP BY employee.employee_code, employee.payment_cycle
ORDER BY employee.employee_code;

DROP TEMPORARY TABLE IF EXISTS tmp_july_cycle_employees;
DROP TEMPORARY TABLE IF EXISTS tmp_july_cycle_dates;
