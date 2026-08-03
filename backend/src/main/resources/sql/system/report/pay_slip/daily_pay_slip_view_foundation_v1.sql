-- =====================================================
-- 日次支払明細 View 基盤 V1
-- payment_date + employee を1明細とし、同一支払日に含まれる
-- 複数の日報を集計する。可変手当・控除は最大10項目まで帳票へ展開する。
-- =====================================================

SET NAMES utf8mb4;

CREATE OR REPLACE VIEW vw_daily_pay_slip_item_source AS
SELECT
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    'ALLOWANCE' AS item_type,
    dra.allowance_code AS item_code,
    dra.allowance_name AS item_name,
    SUM(dra.amount) AS item_value
FROM daily_report dr
JOIN daily_report_allowances dra
  ON dra.daily_report_id = dr.id
 AND dra.deleted_at IS NULL
WHERE dr.deleted_at IS NULL
  AND dr.payment_date IS NOT NULL
GROUP BY
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    dra.allowance_code,
    dra.allowance_name

UNION ALL

SELECT
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    'DEDUCTION' AS item_type,
    drd.deduction_code AS item_code,
    drd.deduction_name AS item_name,
    SUM(drd.amount) AS item_value
FROM daily_report dr
JOIN daily_report_deductions drd
  ON drd.daily_report_id = dr.id
 AND drd.deleted_at IS NULL
WHERE dr.deleted_at IS NULL
  AND dr.payment_date IS NOT NULL
GROUP BY
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    drd.deduction_code,
    drd.deduction_name;

CREATE OR REPLACE VIEW vw_daily_pay_slip_item_ranked AS
SELECT
    source.*,
    ROW_NUMBER() OVER (
        PARTITION BY
            source.tenant_id,
            source.payment_date,
            source.employee_id,
            source.item_type
        ORDER BY source.item_code, source.item_name
    ) AS item_no
FROM vw_daily_pay_slip_item_source source;

CREATE OR REPLACE VIEW vw_daily_pay_slip_work_summary AS
SELECT
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    MIN(dr.work_date) AS labor_period_from,
    MAX(dr.work_date) AS labor_period_to,
    COALESCE(SUM(dr.work_hours), 0) AS work_hours,
    COALESCE(SUM(dr.overtime_hours), 0) AS overtime_hours,
    COALESCE(SUM(dr.night_work_hours), 0) AS night_work_hours,
    COALESCE(SUM(dr.estimated_gross_pay_amount - dr.allowance_amount), 0)
        AS basic_salary,
    COALESCE(SUM(dr.allowance_amount), 0) AS allowance_total,
    COALESCE(SUM(dr.deduction_amount), 0) AS deduction_total,
    COALESCE(SUM(dr.estimated_gross_pay_amount), 0) AS gross_amount
FROM daily_report dr
WHERE dr.deleted_at IS NULL
  AND dr.payment_date IS NOT NULL
GROUP BY dr.tenant_id, dr.payment_date, dr.employee_id;

CREATE OR REPLACE VIEW vw_daily_pay_slip_latest AS
SELECT
    dp.tenant_id,
    dp.payment_date,
    dp.employee_id,
    COALESCE(dp.employee_code, e.employee_code) AS employee_code,
    COALESCE(dp.employee_name, e.employee_name) AS employee_name,
    e.email AS recipient_email,
    COALESCE(work.labor_period_from, dp.payment_date) AS labor_period_from,
    COALESCE(work.labor_period_to, dp.payment_date) AS labor_period_to,
    COALESCE(work.work_hours, 0) AS work_hours,
    COALESCE(work.overtime_hours, 0) AS overtime_hours,
    COALESCE(work.night_work_hours, 0) AS night_work_hours,
    COALESCE(work.basic_salary, dp.planned_amount, 0) AS basic_salary,
    COALESCE(work.allowance_total, 0) AS allowance_total,
    COALESCE(work.deduction_total, 0) AS deduction_total,
    COALESCE(work.gross_amount, dp.planned_amount, 0)
        AS gross_amount,
    COALESCE(dp.actual_amount, 0) AS daily_payment_amount,
    COALESCE(dp.actual_amount, 0) AS net_payment_amount,
    dp.note,

    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 1 THEN item.item_name END) AS allowance_item_name1,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 1 THEN item.item_value END) AS allowance_item_value1,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 2 THEN item.item_name END) AS allowance_item_name2,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 2 THEN item.item_value END) AS allowance_item_value2,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 3 THEN item.item_name END) AS allowance_item_name3,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 3 THEN item.item_value END) AS allowance_item_value3,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 4 THEN item.item_name END) AS allowance_item_name4,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 4 THEN item.item_value END) AS allowance_item_value4,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 5 THEN item.item_name END) AS allowance_item_name5,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 5 THEN item.item_value END) AS allowance_item_value5,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 6 THEN item.item_name END) AS allowance_item_name6,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 6 THEN item.item_value END) AS allowance_item_value6,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 7 THEN item.item_name END) AS allowance_item_name7,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 7 THEN item.item_value END) AS allowance_item_value7,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 8 THEN item.item_name END) AS allowance_item_name8,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 8 THEN item.item_value END) AS allowance_item_value8,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 9 THEN item.item_name END) AS allowance_item_name9,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 9 THEN item.item_value END) AS allowance_item_value9,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 10 THEN item.item_name END) AS allowance_item_name10,
    MAX(CASE WHEN item.item_type = 'ALLOWANCE' AND item.item_no = 10 THEN item.item_value END) AS allowance_item_value10,

    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 1 THEN item.item_name END) AS deduction_item_name1,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 1 THEN item.item_value END) AS deduction_item_value1,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 2 THEN item.item_name END) AS deduction_item_name2,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 2 THEN item.item_value END) AS deduction_item_value2,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 3 THEN item.item_name END) AS deduction_item_name3,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 3 THEN item.item_value END) AS deduction_item_value3,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 4 THEN item.item_name END) AS deduction_item_name4,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 4 THEN item.item_value END) AS deduction_item_value4,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 5 THEN item.item_name END) AS deduction_item_name5,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 5 THEN item.item_value END) AS deduction_item_value5,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 6 THEN item.item_name END) AS deduction_item_name6,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 6 THEN item.item_value END) AS deduction_item_value6,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 7 THEN item.item_name END) AS deduction_item_name7,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 7 THEN item.item_value END) AS deduction_item_value7,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 8 THEN item.item_name END) AS deduction_item_name8,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 8 THEN item.item_value END) AS deduction_item_value8,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 9 THEN item.item_name END) AS deduction_item_name9,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 9 THEN item.item_value END) AS deduction_item_value9,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 10 THEN item.item_name END) AS deduction_item_name10,
    MAX(CASE WHEN item.item_type = 'DEDUCTION' AND item.item_no = 10 THEN item.item_value END) AS deduction_item_value10
FROM daily_payments dp
JOIN employee e
  ON e.tenant_id = dp.tenant_id
 AND e.id = dp.employee_id
 AND e.deleted_at IS NULL
LEFT JOIN vw_daily_pay_slip_work_summary work
  ON work.tenant_id = dp.tenant_id
 AND work.employee_id = dp.employee_id
 AND work.payment_date = dp.payment_date
LEFT JOIN vw_daily_pay_slip_item_ranked item
  ON item.tenant_id = dp.tenant_id
 AND item.payment_date = dp.payment_date
 AND item.employee_id = dp.employee_id
WHERE dp.deleted_at IS NULL
GROUP BY
    dp.tenant_id,
    dp.payment_date,
    dp.employee_id,
    dp.employee_code,
    dp.employee_name,
    e.employee_code,
    e.employee_name,
    e.email,
    dp.planned_amount,
    dp.actual_amount,
    dp.note,
    work.labor_period_from,
    work.labor_period_to,
    work.work_hours,
    work.overtime_hours,
    work.night_work_hours,
    work.basic_salary,
    work.allowance_total,
    work.deduction_total,
    work.gross_amount;
