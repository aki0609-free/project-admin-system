-- ProjectAdmin 月次給与明細 View基盤 V1
-- MySQL 8.x
--
-- 目的:
--   1. 月次給与明細を「従業員 x 対象月」で1行にする。
--   2. 日報に確定保存された可変手当・控除を、表示順で最大12枠へ展開する。
--   3. 日次支払のうち PAID の actual_amount を「前払い」として月次控除へ集計する。
--   4. 月次締めストアドが、このViewをhistoryTableへスナップショットできるようにする。

SET NAMES utf8mb4;
--
-- 重要:
--   - 本DDLは既存テーブルを変更しない追加Viewである。
--   - JEXL / MVEL / Java Bean のAUTO RuleはMySQL View内では実行できない。
--   - 社会保険・所得税の確定計算は、給与マスター確定後に専用SQL Viewを接続する。
--   - item_countが12を超えた場合、締め処理側でエラーにし、項目を黙って欠落させない。

CREATE TABLE IF NOT EXISTS employee_standard_remuneration (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    health_standard_remuneration DECIMAL(15, 2) NOT NULL,
    pension_standard_remuneration DECIMAL(15, 2) NOT NULL,
    source_type VARCHAR(30) NOT NULL DEFAULT 'REGULAR_DECISION',
    note VARCHAR(1000) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_standard_remuneration_from
        UNIQUE (tenant_id, employee_id, effective_from),
    INDEX idx_employee_standard_remuneration_period
        (tenant_id, employee_id, effective_from, effective_to),
    CONSTRAINT fk_employee_standard_remuneration_employee
        FOREIGN KEY (employee_id)
        REFERENCES employee (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payroll_calculation_period (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_month DATE NOT NULL,
    income_tax_year INT NOT NULL,
    insurance_rate_year INT NOT NULL,
    child_care_support_required BOOLEAN NOT NULL DEFAULT FALSE,
    rounding_mode VARCHAR(30) NOT NULL DEFAULT 'HALF_UP',
    verified_flag BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP(6) NULL,
    verified_by VARCHAR(100) NULL,
    source_note VARCHAR(1000) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payroll_calculation_period
        UNIQUE (tenant_id, target_month),
    INDEX idx_payroll_calculation_period_ready
        (tenant_id, verified_flag, target_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_employee_month AS
SELECT
    mc.tenant_id,
    mc.target_month,
    COALESCE(
        mc.closing_start_date,
        DATE_FORMAT(mc.target_month, '%Y-%m-01')
    ) AS period_from,
    COALESCE(
        mc.closing_end_date,
        LAST_DAY(mc.target_month)
    ) AS period_to,
    e.id AS employee_id,
    e.employee_code,
    e.employee_name,
    e.email AS recipient_email,
    COALESCE(ec.monthly_salary, 0) AS basic_salary,
    COALESCE(epp.commute_allowance_monthly, 0) AS commute_allowance_monthly,
    CASE
        WHEN epp.resident_tax_calc_flag = TRUE
            THEN COALESCE(rtm.tax_amount, epp.resident_tax_monthly, 0)
        ELSE 0
    END AS resident_tax
FROM monthly_closings mc
JOIN employee e
  ON e.tenant_id = mc.tenant_id
 AND e.deleted_at IS NULL
 AND e.active_flag = TRUE
 AND (e.hire_date IS NULL OR e.hire_date <= COALESCE(mc.closing_end_date, LAST_DAY(mc.target_month)))
 AND (e.resign_date IS NULL OR e.resign_date >= COALESCE(mc.closing_start_date, DATE_FORMAT(mc.target_month, '%Y-%m-01')))
JOIN employee_contract ec
  ON ec.tenant_id = mc.tenant_id
 AND ec.employee_id = e.id
 AND ec.deleted_at IS NULL
 AND ec.salary_type = 'MONTHLY'
LEFT JOIN employee_payroll_profile epp
  ON epp.tenant_id = mc.tenant_id
 AND epp.employee_id = e.id
 AND epp.deleted_at IS NULL
LEFT JOIN resident_tax_monthly rtm
  ON rtm.tenant_id = mc.tenant_id
 AND rtm.employee_id = e.id
 AND rtm.fiscal_year = YEAR(mc.target_month)
     - CASE WHEN MONTH(mc.target_month) < 6 THEN 1 ELSE 0 END
 AND rtm.month = MONTH(mc.target_month)
 AND rtm.deleted_at IS NULL
WHERE mc.deleted_at IS NULL;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_attendance AS
SELECT
    em.tenant_id,
    em.target_month,
    em.employee_id,
    COUNT(DISTINCT dr.work_date) AS work_day_count,
    COALESCE(SUM(dr.work_hours), 0) AS work_hours,
    COALESCE(SUM(dr.overtime_hours), 0) AS overtime_hours,
    COALESCE(SUM(dr.night_work_hours), 0) AS night_work_hours,
    COALESCE(SUM(dr.holiday_work_hours), 0) AS holiday_work_hours,
    COALESCE(SUM(dr.paid_leave_days), 0) AS paid_leave_days,
    COALESCE(SUM(dr.allowance_amount), 0) AS daily_allowance_summary,
    COALESCE(SUM(dr.deduction_amount), 0) AS daily_deduction_summary,
    COALESCE(SUM(dr.saving_amount), 0) AS saving_amount,
    COALESCE(SUM(dr.loan_repayment_amount), 0) AS loan_repayment_amount
FROM vw_monthly_pay_slip_employee_month em
LEFT JOIN daily_report dr
  ON dr.tenant_id = em.tenant_id
 AND dr.employee_id = em.employee_id
 AND dr.deleted_at IS NULL
 AND dr.approval_status = 'APPROVED'
 AND dr.work_date BETWEEN em.period_from AND em.period_to
GROUP BY
    em.tenant_id,
    em.target_month,
    em.employee_id;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_advance AS
SELECT
    em.tenant_id,
    em.target_month,
    em.employee_id,
    COALESCE(SUM(dp.actual_amount), 0) AS advance_payment_amount
FROM vw_monthly_pay_slip_employee_month em
LEFT JOIN daily_payments dp
  ON dp.tenant_id = em.tenant_id
 AND dp.employee_id = em.employee_id
 AND dp.deleted_at IS NULL
 AND dp.status = 'PAID'
 AND dp.payment_date BETWEEN em.period_from AND em.period_to
GROUP BY
    em.tenant_id,
    em.target_month,
    em.employee_id;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_variable_item_source AS
WITH
daily_pay_component AS (
    SELECT
        em.tenant_id,
        em.target_month,
        em.employee_id,
        'ALLOWANCE' AS item_category,
        component.item_code,
        component.item_name,
        component.display_order,
        SUM(component.item_value) AS item_value
    FROM vw_monthly_pay_slip_employee_month em
    JOIN (
        SELECT
            dr.tenant_id,
            dr.employee_id,
            dr.work_date,
            'OVERTIME_PAY' AS item_code,
            '早出・残業金額' AS item_name,
            100 AS display_order,
            COALESCE(dr.overtime_pay_amount, 0) AS item_value
        FROM daily_report dr
        WHERE dr.deleted_at IS NULL
          AND dr.approval_status = 'APPROVED'
        UNION ALL
        SELECT
            dr.tenant_id,
            dr.employee_id,
            dr.work_date,
            'NIGHT_PAY' AS item_code,
            '深夜金額' AS item_name,
            110 AS display_order,
            COALESCE(dr.night_pay_amount, 0) AS item_value
        FROM daily_report dr
        WHERE dr.deleted_at IS NULL
          AND dr.approval_status = 'APPROVED'
        UNION ALL
        SELECT
            dr.tenant_id,
            dr.employee_id,
            dr.work_date,
            'HOLIDAY_PAY' AS item_code,
            '休日金額' AS item_name,
            120 AS display_order,
            COALESCE(dr.holiday_pay_amount, 0) AS item_value
        FROM daily_report dr
        WHERE dr.deleted_at IS NULL
          AND dr.approval_status = 'APPROVED'
    ) component
      ON component.tenant_id = em.tenant_id
     AND component.employee_id = em.employee_id
     AND component.work_date BETWEEN em.period_from AND em.period_to
    WHERE component.item_value <> 0
    GROUP BY
        em.tenant_id,
        em.target_month,
        em.employee_id,
        component.item_code,
        component.item_name,
        component.display_order
),
daily_allowance AS (
    SELECT
        em.tenant_id,
        em.target_month,
        em.employee_id,
        'ALLOWANCE' AS item_category,
        dra.allowance_code AS item_code,
        MAX(dra.allowance_name) AS item_name,
        COALESCE(am.display_order, 9000) AS display_order,
        SUM(dra.amount) AS item_value
    FROM vw_monthly_pay_slip_employee_month em
    JOIN daily_report dr
      ON dr.tenant_id = em.tenant_id
     AND dr.employee_id = em.employee_id
     AND dr.deleted_at IS NULL
     AND dr.approval_status = 'APPROVED'
     AND dr.work_date BETWEEN em.period_from AND em.period_to
    JOIN daily_report_allowances dra
      ON dra.tenant_id = em.tenant_id
     AND dra.daily_report_id = dr.id
     AND dra.deleted_at IS NULL
    LEFT JOIN allowance_masters am
      ON am.tenant_id = em.tenant_id
     AND am.id = dra.allowance_master_id
     AND am.deleted_at IS NULL
    GROUP BY
        em.tenant_id,
        em.target_month,
        em.employee_id,
        dra.allowance_code,
        am.display_order
),
commute_allowance AS (
    SELECT
        tenant_id,
        target_month,
        employee_id,
        'ALLOWANCE' AS item_category,
        'COMMUTE_ALLOWANCE_MONTHLY' AS item_code,
        '通勤手当' AS item_name,
        8000 AS display_order,
        commute_allowance_monthly AS item_value
    FROM vw_monthly_pay_slip_employee_month
    WHERE commute_allowance_monthly <> 0
),
daily_deduction AS (
    SELECT
        em.tenant_id,
        em.target_month,
        em.employee_id,
        CASE
            WHEN dm.deduction_type = 'LEGAL'
                THEN 'LEGAL_DEDUCTION'
            ELSE 'OTHER_DEDUCTION'
        END AS item_category,
        drd.deduction_code AS item_code,
        MAX(drd.deduction_name) AS item_name,
        COALESCE(dm.display_order, 9000) AS display_order,
        SUM(drd.amount) AS item_value
    FROM vw_monthly_pay_slip_employee_month em
    JOIN daily_report dr
      ON dr.tenant_id = em.tenant_id
     AND dr.employee_id = em.employee_id
     AND dr.deleted_at IS NULL
     AND dr.approval_status = 'APPROVED'
     AND dr.work_date BETWEEN em.period_from AND em.period_to
    JOIN daily_report_deductions drd
      ON drd.tenant_id = em.tenant_id
     AND drd.daily_report_id = dr.id
     AND drd.deleted_at IS NULL
    LEFT JOIN deduction_masters dm
      ON dm.tenant_id = em.tenant_id
     AND dm.id = drd.deduction_master_id
     AND dm.deleted_at IS NULL
    GROUP BY
        em.tenant_id,
        em.target_month,
        em.employee_id,
        CASE
            WHEN dm.deduction_type = 'LEGAL'
                THEN 'LEGAL_DEDUCTION'
            ELSE 'OTHER_DEDUCTION'
        END,
        drd.deduction_code,
        dm.display_order
),
advance_payment AS (
    SELECT
        tenant_id,
        target_month,
        employee_id,
        'OTHER_DEDUCTION' AS item_category,
        'ADVANCE_PAYMENT' AS item_code,
        '前払い' AS item_name,
        1000 AS display_order,
        advance_payment_amount AS item_value
    FROM vw_monthly_pay_slip_advance
    WHERE advance_payment_amount <> 0
),
saving AS (
    SELECT
        tenant_id,
        target_month,
        employee_id,
        'OTHER_DEDUCTION' AS item_category,
        'SAVING' AS item_code,
        '貯金額' AS item_name,
        2000 AS display_order,
        saving_amount AS item_value
    FROM vw_monthly_pay_slip_attendance
    WHERE saving_amount <> 0
),
loan_repayment AS (
    SELECT
        tenant_id,
        target_month,
        employee_id,
        'OTHER_DEDUCTION' AS item_category,
        'LOAN_REPAYMENT' AS item_code,
        '借入金返済額' AS item_name,
        3000 AS display_order,
        loan_repayment_amount AS item_value
    FROM vw_monthly_pay_slip_attendance
    WHERE loan_repayment_amount <> 0
)
SELECT * FROM daily_pay_component
UNION ALL
SELECT * FROM daily_allowance
UNION ALL
SELECT * FROM commute_allowance
UNION ALL
SELECT * FROM daily_deduction
UNION ALL
SELECT * FROM advance_payment
UNION ALL
SELECT * FROM saving
UNION ALL
SELECT * FROM loan_repayment;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_variable_item AS
SELECT
    source.tenant_id,
    source.target_month,
    source.employee_id,
    source.item_category,
    source.item_code,
    source.item_name,
    source.item_value,
    source.display_order,
    ROW_NUMBER() OVER (
        PARTITION BY
            source.tenant_id,
            source.target_month,
            source.employee_id,
            source.item_category
        ORDER BY
            source.display_order,
            source.item_code
    ) AS item_no
FROM vw_monthly_pay_slip_variable_item_source source;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_gross_basis AS
SELECT
    em.tenant_id,
    em.target_month,
    em.employee_id,
    em.basic_salary,
    COALESCE(SUM(
        CASE
            WHEN item.item_category = 'ALLOWANCE'
                THEN item.item_value
            ELSE 0
        END
    ), 0) AS allowance_total,
    em.basic_salary
        + COALESCE(SUM(
            CASE
                WHEN item.item_category = 'ALLOWANCE'
                    THEN item.item_value
                ELSE 0
            END
        ), 0) AS gross_amount
FROM vw_monthly_pay_slip_employee_month em
LEFT JOIN vw_monthly_pay_slip_variable_item_source item
  ON item.tenant_id = em.tenant_id
 AND item.target_month = em.target_month
 AND item.employee_id = em.employee_id
GROUP BY
    em.tenant_id,
    em.target_month,
    em.employee_id,
    em.basic_salary;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_tax_calculation AS
WITH
insurance_rate AS (
    SELECT
        year,
        MAX(CASE WHEN insurance_type = 'HEALTH_INSURANCE' THEN employee_rate END)
            AS health_insurance_rate,
        MAX(CASE WHEN insurance_type = 'CARE_INSURANCE' THEN employee_rate END)
            AS care_insurance_rate,
        MAX(CASE WHEN insurance_type = 'PENSION' THEN employee_rate END)
            AS pension_insurance_rate,
        MAX(CASE WHEN insurance_type = 'EMPLOYMENT_INSURANCE' THEN employee_rate END)
            AS employment_insurance_rate,
        MAX(CASE WHEN insurance_type = 'CHILD_CARE_SUPPORT' THEN employee_rate END)
            AS child_care_support_rate
    FROM insurance_rate_master
    GROUP BY year
),
standard_remuneration_candidate AS (
    SELECT
        em.tenant_id,
        em.target_month,
        em.employee_id,
        remuneration.health_standard_remuneration,
        remuneration.pension_standard_remuneration,
        COUNT(remuneration.id) OVER (
            PARTITION BY
                em.tenant_id,
                em.target_month,
                em.employee_id
        ) AS remuneration_match_count,
        ROW_NUMBER() OVER (
            PARTITION BY
                em.tenant_id,
                em.target_month,
                em.employee_id
            ORDER BY
                remuneration.effective_from DESC,
                remuneration.id DESC
        ) AS remuneration_row_no
    FROM vw_monthly_pay_slip_employee_month em
    LEFT JOIN employee_standard_remuneration remuneration
      ON remuneration.tenant_id = em.tenant_id
     AND remuneration.employee_id = em.employee_id
     AND remuneration.deleted_at IS NULL
     AND em.period_to >= remuneration.effective_from
     AND (
         remuneration.effective_to IS NULL
         OR em.period_from <= remuneration.effective_to
     )
),
calculation_basis AS (
    SELECT
        em.tenant_id,
        em.target_month,
        em.employee_id,
        gross.gross_amount,
        period.id AS calculation_period_id,
        period.income_tax_year,
        period.insurance_rate_year,
        period.child_care_support_required,
        period.rounding_mode,
        period.verified_flag AS calculation_period_verified,
        epp.id AS payroll_profile_id,
        COALESCE(epp.tax_category, 'KOU') AS tax_category,
        COALESCE(epp.tax_dependent_count, 0) AS tax_dependent_count,
        COALESCE(epp.income_tax_calc_flag, TRUE) AS income_tax_calc_flag,
        COALESCE(epp.employment_insurance_flag, TRUE) AS employment_insurance_flag,
        COALESCE(epp.social_insurance_flag, TRUE) AS social_insurance_flag,
        COALESCE(epp.health_insurance_flag, TRUE) AS health_insurance_flag,
        COALESCE(epp.pension_insurance_flag, TRUE) AS pension_insurance_flag,
        COALESCE(epp.care_insurance_flag, FALSE) AS care_insurance_flag,
        em.resident_tax,
        remuneration.health_standard_remuneration,
        remuneration.pension_standard_remuneration,
        remuneration.remuneration_match_count,
        rate.health_insurance_rate,
        rate.care_insurance_rate,
        rate.pension_insurance_rate,
        rate.employment_insurance_rate,
        rate.child_care_support_rate
    FROM vw_monthly_pay_slip_employee_month em
    JOIN vw_monthly_pay_slip_gross_basis gross
      ON gross.tenant_id = em.tenant_id
     AND gross.target_month = em.target_month
     AND gross.employee_id = em.employee_id
    LEFT JOIN employee_payroll_profile epp
      ON epp.tenant_id = em.tenant_id
     AND epp.employee_id = em.employee_id
     AND epp.deleted_at IS NULL
    LEFT JOIN payroll_calculation_period period
      ON period.tenant_id = em.tenant_id
     AND period.target_month = em.target_month
     AND period.deleted_at IS NULL
    LEFT JOIN standard_remuneration_candidate remuneration
      ON remuneration.tenant_id = em.tenant_id
     AND remuneration.target_month = em.target_month
     AND remuneration.employee_id = em.employee_id
     AND remuneration.remuneration_row_no = 1
    LEFT JOIN insurance_rate rate
      ON rate.year = period.insurance_rate_year
),
insurance_amount AS (
    SELECT
        basis.*,
        CASE
            WHEN basis.social_insurance_flag
             AND basis.health_insurance_flag
             AND basis.health_standard_remuneration IS NOT NULL
             AND basis.health_insurance_rate IS NOT NULL
                THEN ROUND(
                    basis.health_standard_remuneration
                        * basis.health_insurance_rate,
                    0
                )
                + CASE
                    WHEN basis.care_insurance_flag
                     AND basis.care_insurance_rate IS NOT NULL
                        THEN ROUND(
                            basis.health_standard_remuneration
                                * basis.care_insurance_rate,
                            0
                        )
                    ELSE 0
                  END
            ELSE 0
        END AS health_insurance,
        CASE
            WHEN basis.social_insurance_flag
             AND basis.health_insurance_flag
             AND basis.child_care_support_required
             AND basis.health_standard_remuneration IS NOT NULL
             AND basis.child_care_support_rate IS NOT NULL
                THEN ROUND(
                    basis.health_standard_remuneration
                        * basis.child_care_support_rate,
                    0
                )
            ELSE 0
        END AS child_care_contribution,
        CASE
            WHEN basis.social_insurance_flag
             AND basis.pension_insurance_flag
             AND basis.pension_standard_remuneration IS NOT NULL
             AND basis.pension_insurance_rate IS NOT NULL
                THEN ROUND(
                    basis.pension_standard_remuneration
                        * basis.pension_insurance_rate,
                    0
                )
            ELSE 0
        END AS pension_insurance,
        CASE
            WHEN basis.employment_insurance_flag
             AND basis.employment_insurance_rate IS NOT NULL
                THEN ROUND(basis.gross_amount * basis.employment_insurance_rate, 0)
            ELSE 0
        END AS employment_insurance
    FROM calculation_basis basis
),
taxable_amount AS (
    SELECT
        amount.*,
        amount.health_insurance
            + amount.child_care_contribution
            + amount.pension_insurance
            + amount.employment_insurance
            AS social_insurance_total,
        GREATEST(
            amount.gross_amount
                - amount.health_insurance
                - amount.child_care_contribution
                - amount.pension_insurance
                - amount.employment_insurance,
            0
        ) AS taxable_amount
    FROM insurance_amount amount
)
SELECT
    taxable.tenant_id,
    taxable.target_month,
    taxable.employee_id,
    taxable.health_standard_remuneration,
    taxable.pension_standard_remuneration,
    taxable.health_insurance,
    taxable.child_care_contribution,
    taxable.pension_insurance,
    taxable.employment_insurance,
    taxable.social_insurance_total,
    taxable.taxable_amount,
    CASE
        WHEN taxable.income_tax_calc_flag = FALSE THEN 0
        WHEN taxable.tax_category = 'KOU' THEN income_tax.tax_amount
        ELSE NULL
    END AS income_tax,
    taxable.resident_tax,
    CASE
        WHEN taxable.payroll_profile_id IS NULL
            THEN 'PAYROLL_PROFILE_MISSING'
        WHEN taxable.calculation_period_id IS NULL
            THEN 'CALCULATION_PERIOD_MISSING'
        WHEN taxable.calculation_period_verified = FALSE
            THEN 'CALCULATION_PERIOD_NOT_VERIFIED'
        WHEN taxable.rounding_mode <> 'HALF_UP'
            THEN 'ROUNDING_MODE_UNSUPPORTED'
        WHEN taxable.social_insurance_flag = TRUE
         AND taxable.health_insurance_flag = TRUE
         AND taxable.care_insurance_flag = TRUE
         AND taxable.care_insurance_rate IS NULL
            THEN 'CARE_INSURANCE_RATE_MISSING'
        WHEN taxable.social_insurance_flag = TRUE
         AND COALESCE(taxable.remuneration_match_count, 0) = 0
            THEN 'STANDARD_REMUNERATION_MISSING'
        WHEN taxable.social_insurance_flag = TRUE
         AND taxable.remuneration_match_count > 1
            THEN 'STANDARD_REMUNERATION_OVERLAPPED'
        WHEN taxable.social_insurance_flag = TRUE
         AND taxable.health_insurance_flag = TRUE
         AND taxable.health_insurance_rate IS NULL
            THEN 'HEALTH_INSURANCE_RATE_MISSING'
        WHEN taxable.social_insurance_flag = TRUE
         AND taxable.pension_insurance_flag = TRUE
         AND taxable.pension_insurance_rate IS NULL
            THEN 'PENSION_INSURANCE_RATE_MISSING'
        WHEN taxable.employment_insurance_flag = TRUE
         AND taxable.employment_insurance_rate IS NULL
            THEN 'EMPLOYMENT_INSURANCE_RATE_MISSING'
        WHEN taxable.social_insurance_flag = TRUE
         AND taxable.health_insurance_flag = TRUE
         AND taxable.child_care_support_required = TRUE
         AND taxable.child_care_support_rate IS NULL
            THEN 'CHILD_CARE_SUPPORT_RATE_MISSING'
        WHEN taxable.income_tax_calc_flag = TRUE
         AND taxable.tax_category <> 'KOU'
            THEN 'INCOME_TAX_CATEGORY_UNSUPPORTED'
        WHEN taxable.income_tax_calc_flag = TRUE
         AND income_tax.id IS NULL
            THEN 'INCOME_TAX_BRACKET_MISSING'
        ELSE NULL
    END AS calculation_error_code,
    CASE
        WHEN taxable.payroll_profile_id IS NULL THEN FALSE
        WHEN taxable.calculation_period_id IS NULL THEN FALSE
        WHEN taxable.calculation_period_verified = FALSE THEN FALSE
        WHEN taxable.rounding_mode <> 'HALF_UP' THEN FALSE
        WHEN taxable.social_insurance_flag = TRUE
         AND taxable.health_insurance_flag = TRUE
         AND taxable.care_insurance_flag = TRUE
         AND taxable.care_insurance_rate IS NULL THEN FALSE
        WHEN taxable.social_insurance_flag = TRUE
         AND COALESCE(taxable.remuneration_match_count, 0) <> 1 THEN FALSE
        WHEN taxable.social_insurance_flag = TRUE
         AND taxable.health_insurance_flag = TRUE
         AND taxable.health_insurance_rate IS NULL THEN FALSE
        WHEN taxable.social_insurance_flag = TRUE
         AND taxable.pension_insurance_flag = TRUE
         AND taxable.pension_insurance_rate IS NULL THEN FALSE
        WHEN taxable.employment_insurance_flag = TRUE
         AND taxable.employment_insurance_rate IS NULL THEN FALSE
        WHEN taxable.social_insurance_flag = TRUE
         AND taxable.health_insurance_flag = TRUE
         AND taxable.child_care_support_required = TRUE
         AND taxable.child_care_support_rate IS NULL THEN FALSE
        WHEN taxable.income_tax_calc_flag = TRUE
         AND taxable.tax_category <> 'KOU' THEN FALSE
        WHEN taxable.income_tax_calc_flag = TRUE
         AND income_tax.id IS NULL THEN FALSE
        ELSE TRUE
    END AS calculation_ready
FROM taxable_amount taxable
LEFT JOIN income_tax_table income_tax
  ON income_tax.year = taxable.income_tax_year
 AND income_tax.dependents = taxable.tax_dependent_count
 AND taxable.taxable_amount BETWEEN income_tax.min_salary AND income_tax.max_salary;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_company AS
SELECT
    profile.tenant_id,
    profile.company_name
FROM company_profile profile
JOIN (
    SELECT
        tenant_id,
        MIN(id) AS company_profile_id
    FROM company_profile
    WHERE active_flag = TRUE
      AND deleted_at IS NULL
    GROUP BY tenant_id
) selected
  ON selected.company_profile_id = profile.id
 AND selected.tenant_id = profile.tenant_id;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_latest AS
SELECT
    em.tenant_id,
    em.target_month,
    em.period_from,
    em.period_to,
    em.employee_id,
    em.employee_code,
    em.employee_name,
    em.recipient_email,
    company.company_name,
    CONCAT(
        'MONTHLY_PAY_SLIP:',
        DATE_FORMAT(em.target_month, '%Y-%m'),
        ':',
        em.employee_id
    ) AS business_key,

    COALESCE(att.work_day_count, 0) AS work_day_count,
    COALESCE(att.work_hours, 0) AS work_hours,
    COALESCE(att.overtime_hours, 0) AS overtime_hours,
    COALESCE(att.night_work_hours, 0) AS night_work_hours,
    COALESCE(att.holiday_work_hours, 0) AS holiday_work_hours,
    COALESCE(att.paid_leave_days, 0) AS paid_leave_days,

    gross.basic_salary,
    gross.allowance_total,
    gross.gross_amount,
    tax.health_standard_remuneration,
    tax.pension_standard_remuneration,
    tax.health_insurance,
    tax.child_care_contribution,
    tax.pension_insurance,
    tax.employment_insurance,
    tax.social_insurance_total,
    tax.taxable_amount,
    tax.income_tax,
    tax.resident_tax,
    tax.calculation_ready,
    tax.calculation_error_code,
    tax.social_insurance_total
        + COALESCE(tax.income_tax, 0)
        + COALESCE(tax.resident_tax, 0)
        + COALESCE(SUM(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' THEN item.item_value ELSE 0 END), 0)
        AS legal_deduction_total,
    COALESCE(SUM(CASE WHEN item.item_category = 'OTHER_DEDUCTION' THEN item.item_value ELSE 0 END), 0)
        AS other_deduction_total,
    tax.social_insurance_total
        + COALESCE(tax.income_tax, 0)
        + COALESCE(tax.resident_tax, 0)
        + COALESCE(SUM(CASE WHEN item.item_category IN ('LEGAL_DEDUCTION', 'OTHER_DEDUCTION') THEN item.item_value ELSE 0 END), 0)
        AS deduction_total,
    gross.gross_amount
        - tax.social_insurance_total
        - COALESCE(tax.income_tax, 0)
        - COALESCE(tax.resident_tax, 0)
        - COALESCE(SUM(CASE WHEN item.item_category IN ('LEGAL_DEDUCTION', 'OTHER_DEDUCTION') THEN item.item_value ELSE 0 END), 0)
        AS net_payment_amount,
    COALESCE(adv.advance_payment_amount, 0) AS advance_payment_amount,

    COUNT(CASE WHEN item.item_category = 'ALLOWANCE' THEN 1 END) AS allowance_item_count,
    COUNT(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' THEN 1 END) AS legal_deduction_item_count,
    COUNT(CASE WHEN item.item_category = 'OTHER_DEDUCTION' THEN 1 END) AS other_deduction_item_count,

    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 1 THEN item.item_name END) AS allowance_item_name_01,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 1 THEN item.item_value END) AS allowance_item_value_01,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 2 THEN item.item_name END) AS allowance_item_name_02,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 2 THEN item.item_value END) AS allowance_item_value_02,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 3 THEN item.item_name END) AS allowance_item_name_03,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 3 THEN item.item_value END) AS allowance_item_value_03,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 4 THEN item.item_name END) AS allowance_item_name_04,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 4 THEN item.item_value END) AS allowance_item_value_04,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 5 THEN item.item_name END) AS allowance_item_name_05,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 5 THEN item.item_value END) AS allowance_item_value_05,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 6 THEN item.item_name END) AS allowance_item_name_06,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 6 THEN item.item_value END) AS allowance_item_value_06,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 7 THEN item.item_name END) AS allowance_item_name_07,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 7 THEN item.item_value END) AS allowance_item_value_07,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 8 THEN item.item_name END) AS allowance_item_name_08,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 8 THEN item.item_value END) AS allowance_item_value_08,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 9 THEN item.item_name END) AS allowance_item_name_09,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 9 THEN item.item_value END) AS allowance_item_value_09,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 10 THEN item.item_name END) AS allowance_item_name_10,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 10 THEN item.item_value END) AS allowance_item_value_10,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 11 THEN item.item_name END) AS allowance_item_name_11,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 11 THEN item.item_value END) AS allowance_item_value_11,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 12 THEN item.item_name END) AS allowance_item_name_12,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 12 THEN item.item_value END) AS allowance_item_value_12,

    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 1 THEN item.item_name END) AS legal_item_name_01,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 1 THEN item.item_value END) AS legal_item_value_01,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 2 THEN item.item_name END) AS legal_item_name_02,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 2 THEN item.item_value END) AS legal_item_value_02,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 3 THEN item.item_name END) AS legal_item_name_03,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 3 THEN item.item_value END) AS legal_item_value_03,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 4 THEN item.item_name END) AS legal_item_name_04,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 4 THEN item.item_value END) AS legal_item_value_04,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 5 THEN item.item_name END) AS legal_item_name_05,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 5 THEN item.item_value END) AS legal_item_value_05,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 6 THEN item.item_name END) AS legal_item_name_06,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 6 THEN item.item_value END) AS legal_item_value_06,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 7 THEN item.item_name END) AS legal_item_name_07,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 7 THEN item.item_value END) AS legal_item_value_07,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 8 THEN item.item_name END) AS legal_item_name_08,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 8 THEN item.item_value END) AS legal_item_value_08,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 9 THEN item.item_name END) AS legal_item_name_09,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 9 THEN item.item_value END) AS legal_item_value_09,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 10 THEN item.item_name END) AS legal_item_name_10,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 10 THEN item.item_value END) AS legal_item_value_10,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 11 THEN item.item_name END) AS legal_item_name_11,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 11 THEN item.item_value END) AS legal_item_value_11,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 12 THEN item.item_name END) AS legal_item_name_12,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 12 THEN item.item_value END) AS legal_item_value_12,

    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 1 THEN item.item_name END) AS other_item_name_01,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 1 THEN item.item_value END) AS other_item_value_01,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 2 THEN item.item_name END) AS other_item_name_02,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 2 THEN item.item_value END) AS other_item_value_02,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 3 THEN item.item_name END) AS other_item_name_03,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 3 THEN item.item_value END) AS other_item_value_03,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 4 THEN item.item_name END) AS other_item_name_04,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 4 THEN item.item_value END) AS other_item_value_04,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 5 THEN item.item_name END) AS other_item_name_05,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 5 THEN item.item_value END) AS other_item_value_05,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 6 THEN item.item_name END) AS other_item_name_06,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 6 THEN item.item_value END) AS other_item_value_06,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 7 THEN item.item_name END) AS other_item_name_07,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 7 THEN item.item_value END) AS other_item_value_07,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 8 THEN item.item_name END) AS other_item_name_08,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 8 THEN item.item_value END) AS other_item_value_08,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 9 THEN item.item_name END) AS other_item_name_09,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 9 THEN item.item_value END) AS other_item_value_09,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 10 THEN item.item_name END) AS other_item_name_10,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 10 THEN item.item_value END) AS other_item_value_10,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 11 THEN item.item_name END) AS other_item_name_11,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 11 THEN item.item_value END) AS other_item_value_11,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 12 THEN item.item_name END) AS other_item_name_12,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 12 THEN item.item_value END) AS other_item_value_12
FROM vw_monthly_pay_slip_employee_month em
LEFT JOIN vw_monthly_pay_slip_attendance att
  ON att.tenant_id = em.tenant_id
 AND att.target_month = em.target_month
 AND att.employee_id = em.employee_id
LEFT JOIN vw_monthly_pay_slip_advance adv
  ON adv.tenant_id = em.tenant_id
 AND adv.target_month = em.target_month
 AND adv.employee_id = em.employee_id
JOIN vw_monthly_pay_slip_gross_basis gross
  ON gross.tenant_id = em.tenant_id
 AND gross.target_month = em.target_month
 AND gross.employee_id = em.employee_id
JOIN vw_monthly_pay_slip_tax_calculation tax
  ON tax.tenant_id = em.tenant_id
 AND tax.target_month = em.target_month
 AND tax.employee_id = em.employee_id
LEFT JOIN vw_monthly_pay_slip_variable_item item
  ON item.tenant_id = em.tenant_id
 AND item.target_month = em.target_month
 AND item.employee_id = em.employee_id
LEFT JOIN vw_monthly_pay_slip_company company
  ON company.tenant_id = em.tenant_id
GROUP BY
    em.tenant_id,
    em.target_month,
    em.period_from,
    em.period_to,
    em.employee_id,
    em.employee_code,
    em.employee_name,
    em.recipient_email,
    company.company_name,
    gross.basic_salary,
    gross.allowance_total,
    gross.gross_amount,
    tax.health_standard_remuneration,
    tax.pension_standard_remuneration,
    tax.health_insurance,
    tax.child_care_contribution,
    tax.pension_insurance,
    tax.employment_insurance,
    tax.social_insurance_total,
    tax.taxable_amount,
    tax.income_tax,
    tax.resident_tax,
    tax.calculation_ready,
    tax.calculation_error_code,
    att.work_day_count,
    att.work_hours,
    att.overtime_hours,
    att.night_work_hours,
    att.holiday_work_hours,
    att.paid_leave_days,
    adv.advance_payment_amount;
