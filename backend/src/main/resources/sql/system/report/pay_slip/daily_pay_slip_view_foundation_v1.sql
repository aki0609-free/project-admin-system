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
    COALESCE(am.display_order, 9000) AS display_order,
    SUM(dra.amount) AS item_value
FROM daily_report dr
JOIN daily_report_allowances dra
  ON dra.daily_report_id = dr.id
 AND dra.deleted_at IS NULL
LEFT JOIN allowance_masters am
  ON am.tenant_id = dr.tenant_id
 AND am.id = dra.allowance_master_id
 AND am.deleted_at IS NULL
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
  AND dr.payment_date IS NOT NULL
  AND COALESCE(am.show_on_daily_statement, TRUE) = TRUE
GROUP BY
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    dra.allowance_code,
    dra.allowance_name,
    am.display_order

UNION ALL

-- 日次表示対象の手当マスターは、日報に実績明細がなくても0円で表示する。
-- 従業員別適用の場合は、対象となる勤務日に有効な紐付けがある項目だけを採用する。
SELECT
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    'ALLOWANCE' AS item_type,
    am.allowance_code AS item_code,
    am.allowance_name AS item_name,
    COALESCE(am.display_order, 9000) AS display_order,
    0 AS item_value
FROM daily_report dr
JOIN allowance_masters am
  ON am.tenant_id = dr.tenant_id
 AND am.deleted_at IS NULL
 AND am.enabled = TRUE
 AND am.show_on_daily_statement = TRUE
 AND am.allowance_unit IN ('DAILY', 'BOTH')
LEFT JOIN payroll_item_balance_policy policy
  ON policy.tenant_id = am.tenant_id
 AND policy.target_type = 'ALLOWANCE'
 AND policy.target_master_id = am.id
 AND policy.target_code = am.allowance_code
 AND policy.deleted_at IS NULL
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
  AND dr.payment_date IS NOT NULL
  AND (
      policy.id IS NULL
      OR (
          policy.active_flag = TRUE
          AND policy.input_source IN ('DAILY_REPORT', 'DAILY_REPORT_AND_TRANSACTION')
          AND (
              policy.application_scope = 'ALL_EMPLOYEES'
              OR (
                  policy.application_scope = 'EMPLOYEE_ENROLLMENT'
                  AND EXISTS (
                      SELECT 1
                      FROM employee_payroll_item_enrollment enrollment
                      WHERE enrollment.tenant_id = dr.tenant_id
                        AND enrollment.employee_id = dr.employee_id
                        AND enrollment.balance_policy_id = policy.id
                        AND enrollment.deleted_at IS NULL
                        AND enrollment.effective_from <= dr.work_date
                        AND (
                            enrollment.effective_to IS NULL
                            OR enrollment.effective_to >= dr.work_date
                        )
                  )
              )
          )
      )
  )
  AND NOT EXISTS (
      SELECT 1
      FROM daily_report existing_report
      JOIN daily_report_allowances existing_item
        ON existing_item.daily_report_id = existing_report.id
       AND existing_item.allowance_master_id = am.id
       AND existing_item.deleted_at IS NULL
      WHERE existing_report.tenant_id = dr.tenant_id
        AND existing_report.payment_date = dr.payment_date
        AND existing_report.employee_id = dr.employee_id
        AND existing_report.deleted_at IS NULL
        AND existing_report.approval_status = 'APPROVED'
  )
GROUP BY
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    am.allowance_code,
    am.allowance_name,
    am.display_order

UNION ALL

SELECT
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    'ALLOWANCE' AS item_type,
    'OTHER_ALLOWANCE' AS item_code,
    'その他手当' AS item_name,
    9990 AS display_order,
    SUM(
        GREATEST(
            COALESCE(dr.allowance_amount, 0) - COALESCE((
                SELECT SUM(detail.amount)
                FROM daily_report_allowances detail
                WHERE detail.daily_report_id = dr.id
                  AND detail.deleted_at IS NULL
            ), 0),
            0
        )
    ) AS item_value
FROM daily_report dr
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
  AND dr.payment_date IS NOT NULL
GROUP BY dr.tenant_id, dr.payment_date, dr.employee_id
HAVING item_value <> 0

UNION ALL

SELECT
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    'DEDUCTION' AS item_type,
    drd.deduction_code AS item_code,
    drd.deduction_name AS item_name,
    COALESCE(dm.display_order, 9000) AS display_order,
    SUM(drd.amount) AS item_value
FROM daily_report dr
JOIN daily_report_deductions drd
  ON drd.daily_report_id = dr.id
 AND drd.deleted_at IS NULL
LEFT JOIN deduction_masters dm
  ON dm.tenant_id = dr.tenant_id
 AND dm.id = drd.deduction_master_id
 AND dm.deleted_at IS NULL
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
  AND dr.payment_date IS NOT NULL
  AND COALESCE(dm.show_on_daily_statement, TRUE) = TRUE
GROUP BY
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    drd.deduction_code,
    drd.deduction_name,
    dm.display_order

UNION ALL

-- 控除も手当と同じ適用判定で、マスター追加を帳票へ自動反映する。
SELECT
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    'DEDUCTION' AS item_type,
    dm.deduction_code AS item_code,
    dm.deduction_name AS item_name,
    COALESCE(dm.display_order, 9000) AS display_order,
    0 AS item_value
FROM daily_report dr
JOIN deduction_masters dm
  ON dm.tenant_id = dr.tenant_id
 AND dm.deleted_at IS NULL
 AND dm.enabled = TRUE
 AND dm.show_on_daily_statement = TRUE
 AND dm.deduction_unit IN ('DAILY', 'BOTH')
LEFT JOIN payroll_item_balance_policy policy
  ON policy.tenant_id = dm.tenant_id
 AND policy.target_type = 'DEDUCTION'
 AND policy.target_master_id = dm.id
 AND policy.target_code = dm.deduction_code
 AND policy.deleted_at IS NULL
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
  AND dr.payment_date IS NOT NULL
  AND (
      policy.id IS NULL
      OR (
          policy.active_flag = TRUE
          AND policy.input_source IN ('DAILY_REPORT', 'DAILY_REPORT_AND_TRANSACTION')
          AND (
              policy.application_scope = 'ALL_EMPLOYEES'
              OR (
                  policy.application_scope = 'EMPLOYEE_ENROLLMENT'
                  AND EXISTS (
                      SELECT 1
                      FROM employee_payroll_item_enrollment enrollment
                      WHERE enrollment.tenant_id = dr.tenant_id
                        AND enrollment.employee_id = dr.employee_id
                        AND enrollment.balance_policy_id = policy.id
                        AND enrollment.deleted_at IS NULL
                        AND enrollment.effective_from <= dr.work_date
                        AND (
                            enrollment.effective_to IS NULL
                            OR enrollment.effective_to >= dr.work_date
                        )
                  )
              )
          )
      )
  )
  AND NOT EXISTS (
      SELECT 1
      FROM daily_report existing_report
      JOIN daily_report_deductions existing_item
        ON existing_item.daily_report_id = existing_report.id
       AND existing_item.deduction_master_id = dm.id
       AND existing_item.deleted_at IS NULL
      WHERE existing_report.tenant_id = dr.tenant_id
        AND existing_report.payment_date = dr.payment_date
        AND existing_report.employee_id = dr.employee_id
        AND existing_report.deleted_at IS NULL
        AND existing_report.approval_status = 'APPROVED'
  )
GROUP BY
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    dm.deduction_code,
    dm.deduction_name,
    dm.display_order

UNION ALL

SELECT
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    'DEDUCTION' AS item_type,
    'OTHER_DEDUCTION' AS item_code,
    'その他控除' AS item_name,
    9970 AS display_order,
    SUM(
        GREATEST(
            COALESCE(dr.deduction_amount, 0) - COALESCE((
                SELECT SUM(detail.amount)
                FROM daily_report_deductions detail
                WHERE detail.daily_report_id = dr.id
                  AND detail.deleted_at IS NULL
            ), 0),
            0
        )
    ) AS item_value
FROM daily_report dr
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
  AND dr.payment_date IS NOT NULL
GROUP BY dr.tenant_id, dr.payment_date, dr.employee_id
HAVING item_value <> 0

UNION ALL

SELECT
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    'DEDUCTION' AS item_type,
    'EMPLOYEE_SAVING' AS item_code,
    '貯金' AS item_name,
    9980 AS display_order,
    SUM(COALESCE(dr.saving_amount, 0)) AS item_value
FROM daily_report dr
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
  AND dr.payment_date IS NOT NULL
GROUP BY dr.tenant_id, dr.payment_date, dr.employee_id
HAVING item_value <> 0

UNION ALL

SELECT
    dr.tenant_id,
    dr.payment_date,
    dr.employee_id,
    'DEDUCTION' AS item_type,
    'LOAN_REPAYMENT' AS item_code,
    '借入金返済額' AS item_name,
    9990 AS display_order,
    SUM(COALESCE(dr.loan_repayment_amount, 0)) AS item_value
FROM daily_report dr
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
  AND dr.payment_date IS NOT NULL
GROUP BY dr.tenant_id, dr.payment_date, dr.employee_id
HAVING item_value <> 0;

CREATE OR REPLACE VIEW vw_daily_pay_slip_item_ranked AS
SELECT
    source.*,
    ROW_NUMBER() OVER (
        PARTITION BY
            source.tenant_id,
            source.payment_date,
            source.employee_id,
            source.item_type
        ORDER BY source.display_order, source.item_code, source.item_name
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
    COALESCE(SUM(dr.normal_pay_amount), 0) AS basic_salary,
    COALESCE(SUM(dr.allowance_amount), 0) AS allowance_total,
    COALESCE(SUM(
        COALESCE(dr.deduction_amount, 0)
        + COALESCE(dr.saving_amount, 0)
        + COALESCE(dr.loan_repayment_amount, 0)
    ), 0) AS deduction_total,
    COALESCE(SUM(dr.estimated_gross_pay_amount), 0) AS gross_amount,
    COALESCE(SUM(dr.estimated_net_pay_amount), 0) AS net_payment_amount,
    GROUP_CONCAT(
        NULLIF(TRIM(dr.work_description), '')
        ORDER BY dr.work_date
        SEPARATOR ' / '
    ) AS note
FROM daily_report dr
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
  AND dr.payment_date IS NOT NULL
GROUP BY dr.tenant_id, dr.payment_date, dr.employee_id;

CREATE OR REPLACE VIEW vw_daily_pay_slip_latest AS
SELECT
    work.tenant_id,
    work.payment_date,
    work.employee_id,
    e.employee_code,
    e.employee_name,
    e.email AS recipient_email,
    DATE_FORMAT(work.payment_date, '%Y年%c月%e日') AS payment_date_label,
    work.labor_period_from,
    work.labor_period_to,
    DATE_FORMAT(work.labor_period_from, '%Y年%c月%e日') AS labor_period_from_label,
    DATE_FORMAT(work.labor_period_to, '%Y年%c月%e日') AS labor_period_to_label,
    COALESCE(work.work_hours, 0) AS work_hours,
    COALESCE(work.overtime_hours, 0) AS overtime_hours,
    COALESCE(work.night_work_hours, 0) AS night_work_hours,
    COALESCE(work.basic_salary, 0) AS basic_salary,
    COALESCE(work.allowance_total, 0) AS allowance_total,
    COALESCE(work.deduction_total, 0) AS deduction_total,
    COALESCE(work.gross_amount, 0) AS gross_amount,
    COALESCE(work.net_payment_amount, 0) AS daily_payment_amount,
    COALESCE(work.net_payment_amount, 0) AS net_payment_amount,
    COALESCE(legal_deposit.current_balance, 0) AS legal_deposit_balance,
    COALESCE(loan.current_balance, 0) AS loan_balance,
    COALESCE(saving.current_balance, 0) AS saving_balance,
    work.note AS note,

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
FROM vw_daily_pay_slip_work_summary work
JOIN employee e
  ON e.tenant_id = work.tenant_id
 AND e.id = work.employee_id
 AND e.deleted_at IS NULL
JOIN employee_contract contract
  ON contract.tenant_id = work.tenant_id
 AND contract.employee_id = work.employee_id
 AND contract.payment_cycle = 'DAILY'
 AND contract.deleted_at IS NULL
LEFT JOIN vw_daily_pay_slip_item_ranked item
  ON item.tenant_id = work.tenant_id
 AND item.payment_date = work.payment_date
 AND item.employee_id = work.employee_id
LEFT JOIN vw_employee_legal_deposit_balance legal_deposit
  ON legal_deposit.tenant_id = work.tenant_id
 AND legal_deposit.employee_id = work.employee_id
LEFT JOIN (
    SELECT tenant_id, employee_id, SUM(current_balance) AS current_balance
    FROM employee_loan
    WHERE approval_status = 'APPROVED'
      AND deleted_at IS NULL
    GROUP BY tenant_id, employee_id
) loan
  ON loan.tenant_id = work.tenant_id
 AND loan.employee_id = work.employee_id
LEFT JOIN (
    SELECT tenant_id, employee_id, SUM(current_balance) AS current_balance
    FROM employee_saving
    WHERE deleted_at IS NULL
    GROUP BY tenant_id, employee_id
) saving
  ON saving.tenant_id = work.tenant_id
 AND saving.employee_id = work.employee_id
GROUP BY
    work.tenant_id,
    work.payment_date,
    work.employee_id,
    e.employee_code,
    e.employee_name,
    e.email,
    legal_deposit.current_balance,
    loan.current_balance,
    saving.current_balance,
    work.labor_period_from,
    work.labor_period_to,
    work.work_hours,
    work.overtime_hours,
    work.night_work_hours,
    work.basic_salary,
    work.allowance_total,
    work.deduction_total,
    work.gross_amount,
    work.net_payment_amount,
    work.note;
