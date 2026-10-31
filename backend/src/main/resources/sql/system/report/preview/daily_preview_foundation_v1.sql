-- =====================================================
-- 日別HTMLプレビュー帳票基盤 V1
-- MySQL 8.x
-- =====================================================

SET NAMES utf8mb4;

-- Hibernate ddl-auto:updateの前後どちらでも適用できるよう、
-- 不足している列だけを追加する。
SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE operation_report_preview ADD COLUMN report_name VARCHAR(200) NULL AFTER report_code',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_report_preview'
      AND column_name = 'report_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- MySQLのHibernate Dialectが過去のJava enum値だけで生成したENUM列では、
-- 後から追加したHTML_PREVIEW/HTML_PRINTが空文字として保存される。
-- 帳票方式の追加にDB DDLが追随し続けないよう、文字列カラムへ統一する。
ALTER TABLE operation_report_preview
    MODIFY COLUMN output_type VARCHAR(30) NOT NULL;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE operation_report_preview ADD COLUMN html_template_key VARCHAR(1000) NULL AFTER template_name',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_report_preview'
      AND column_name = 'html_template_key'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE operation_report_preview ADD COLUMN html_template_version INT NULL AFTER html_template_key',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_report_preview'
      AND column_name = 'html_template_version'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE operation_report_preview ADD COLUMN html_template_hash VARCHAR(128) NULL AFTER html_template_version',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_report_preview'
      AND column_name = 'html_template_hash'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE operation_report_preview ADD COLUMN filter_column_name VARCHAR(100) NULL AFTER table_name',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_report_preview'
      AND column_name = 'filter_column_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------
-- 日別労務費一覧
-- work_date単位の発生労務費と、payment_date単位の当日支払額を表示する。
-- -----------------------------------------------------
CREATE OR REPLACE VIEW vw_daily_labor_cost_preview AS
WITH labor AS (
    SELECT
        dr.tenant_id,
        dr.work_date AS target_date,
        dr.employee_id,
        COALESCE(SUM(dr.estimated_gross_pay_amount), 0)
            AS saved_gross_payment_amount
    FROM daily_report dr
    WHERE dr.deleted_at IS NULL
      AND dr.approval_status = 'APPROVED'
    GROUP BY dr.tenant_id, dr.work_date, dr.employee_id
), payment AS (
    SELECT
        dr.tenant_id,
        dr.payment_date AS target_date,
        dr.employee_id,
        COALESCE(SUM(dr.estimated_gross_pay_amount), 0)
            AS scheduled_payment_amount
    FROM daily_report dr
    WHERE dr.deleted_at IS NULL
      AND dr.approval_status = 'APPROVED'
      AND dr.payment_date IS NOT NULL
    GROUP BY dr.tenant_id, dr.payment_date, dr.employee_id
), target_employee AS (
    SELECT tenant_id, target_date, employee_id FROM labor
    UNION
    SELECT tenant_id, target_date, employee_id FROM payment
), calculated AS (
    SELECT
        target.tenant_id,
        target.target_date,
        target.employee_id,
        employee.employee_code,
        employee.employee_name,
        COALESCE(contract.payment_cycle, 'MONTHLY') AS payment_cycle,
        COALESCE(contract.salary_type, 'DAILY') AS salary_type,
        CASE
            WHEN labor.employee_id IS NULL THEN 0
            WHEN contract.salary_type = 'MONTHLY'
                THEN ROUND(COALESCE(contract.monthly_salary, 0) / 20, 0)
            WHEN contract.salary_type = 'WEEKLY'
                THEN ROUND(COALESCE(contract.weekly_wage, 0) / 5, 0)
            ELSE COALESCE(labor.saved_gross_payment_amount, 0)
        END AS gross_payment_amount,
        COALESCE(payment.scheduled_payment_amount, 0)
            AS scheduled_payment_amount,
        labor.employee_id IS NOT NULL AS has_labor
    FROM target_employee target
    JOIN employee
      ON employee.tenant_id = target.tenant_id
     AND employee.id = target.employee_id
     AND employee.deleted_at IS NULL
    LEFT JOIN labor
      ON labor.tenant_id = target.tenant_id
     AND labor.target_date = target.target_date
     AND labor.employee_id = target.employee_id
    LEFT JOIN payment
      ON payment.tenant_id = target.tenant_id
     AND payment.target_date = target.target_date
     AND payment.employee_id = target.employee_id
    LEFT JOIN employee_contract contract
      ON contract.id = (
          SELECT candidate.id
          FROM employee_contract candidate
          WHERE candidate.tenant_id = target.tenant_id
            AND candidate.employee_id = target.employee_id
            AND candidate.deleted_at IS NULL
            AND (
                candidate.contract_start_date IS NULL
                OR candidate.contract_start_date <= target.target_date
            )
            AND (
                candidate.contract_end_date IS NULL
                OR candidate.contract_end_date >= target.target_date
            )
          ORDER BY candidate.contract_start_date DESC, candidate.id DESC
          LIMIT 1
      )
), detail AS (
    SELECT
        calculated.tenant_id,
        calculated.target_date,
        calculated.employee_id,
        calculated.employee_code,
        calculated.employee_name,
        calculated.payment_cycle,
        calculated.salary_type,
        calculated.gross_payment_amount,
        CASE calculated.payment_cycle
            WHEN 'DAILY' THEN calculated.gross_payment_amount
            ELSE calculated.scheduled_payment_amount
        END AS payment_amount
    FROM calculated
    WHERE calculated.has_labor
       OR calculated.payment_cycle <> 'DAILY'
)
SELECT
    detail.tenant_id,
    detail.target_date,
    DATE_FORMAT(detail.target_date, '%Y年%m月%d日') AS work_date_label,
    detail.employee_id,
    detail.employee_code,
    detail.employee_name,
    detail.payment_cycle,
    detail.salary_type,
    detail.gross_payment_amount,
    detail.payment_amount,
    SUM(detail.gross_payment_amount) OVER (
        PARTITION BY detail.tenant_id, detail.target_date
    ) AS total_gross_payment_amount,
    SUM(detail.payment_amount) OVER (
        PARTITION BY detail.tenant_id, detail.target_date
    ) AS total_payment_amount
FROM detail;

-- -----------------------------------------------------
-- 給与支払表
-- payment_date単位に、承認済み日報の計算上支給可能額を集計する。
-- -----------------------------------------------------
CREATE OR REPLACE VIEW vw_daily_payment_preparation_preview AS
WITH deduction_by_report AS (
    SELECT
        item.tenant_id,
        item.daily_report_id,
        COALESCE(SUM(CASE WHEN item.deduction_code = 'LEGAL_DEPOSIT'
            THEN item.amount ELSE 0 END), 0) AS legal_deposit_amount,
        COALESCE(SUM(CASE WHEN item.deduction_code = 'SHORT_TERM_ADVANCE'
            THEN item.amount ELSE 0 END), 0) AS short_term_advance_amount,
        COALESCE(SUM(CASE WHEN item.deduction_code = 'DORMITORY_FEE'
            THEN item.amount ELSE 0 END), 0) AS dormitory_fee_amount,
        COALESCE(SUM(CASE WHEN item.deduction_code = 'MOBILE_RENTAL'
            THEN item.amount ELSE 0 END), 0) AS mobile_rental_amount,
        COALESCE(SUM(CASE WHEN item.deduction_code = 'WIFI_FEE'
            THEN item.amount ELSE 0 END), 0) AS wifi_fee_amount,
        COALESCE(SUM(CASE WHEN item.deduction_code NOT IN (
            'LEGAL_DEPOSIT',
            'SHORT_TERM_ADVANCE',
            'DORMITORY_FEE',
            'MOBILE_RENTAL',
            'WIFI_FEE',
            'SAVING',
            'SAVINGS',
            'LOAN_REPAYMENT'
        ) THEN item.amount ELSE 0 END), 0) AS other_deduction_amount,
        COALESCE(SUM(item.amount), 0) AS detail_deduction_amount
    FROM daily_report_deductions item
    WHERE item.deleted_at IS NULL
    GROUP BY item.tenant_id, item.daily_report_id
), report_summary AS (
    SELECT
        dr.tenant_id,
        dr.payment_date AS target_date,
        dr.employee_id,
        COALESCE(SUM(dr.estimated_gross_pay_amount), 0)
            AS gross_payment_amount,
        COALESCE(SUM(dr.allowance_amount), 0) AS allowance_amount,
        COALESCE(SUM(deduction.legal_deposit_amount), 0)
            AS legal_deposit_amount,
        COALESCE(SUM(dr.saving_amount), 0) AS saving_amount,
        COALESCE(SUM(dr.saving_withdrawal_amount), 0)
            AS saving_withdrawal_amount,
        COALESCE(SUM(deduction.short_term_advance_amount), 0)
            AS short_term_advance_amount,
        COALESCE(SUM(deduction.dormitory_fee_amount), 0)
            AS dormitory_fee_amount,
        COALESCE(SUM(deduction.mobile_rental_amount), 0)
            AS mobile_rental_amount,
        COALESCE(SUM(deduction.wifi_fee_amount), 0) AS wifi_fee_amount,
        COALESCE(SUM(dr.loan_repayment_amount), 0)
            AS long_term_loan_repayment_amount,
        COALESCE(SUM(
            COALESCE(deduction.other_deduction_amount, 0)
            + GREATEST(
                COALESCE(dr.deduction_amount, 0)
                    - COALESCE(deduction.detail_deduction_amount, 0),
                0
            )
        ), 0) AS other_deduction_amount
    FROM daily_report dr
    LEFT JOIN deduction_by_report deduction
      ON deduction.tenant_id = dr.tenant_id
     AND deduction.daily_report_id = dr.id
    WHERE dr.deleted_at IS NULL
      AND dr.approval_status = 'APPROVED'
      AND dr.payment_date IS NOT NULL
    GROUP BY dr.tenant_id, dr.payment_date, dr.employee_id
), calculated AS (
    SELECT
        report.*,
        report.gross_payment_amount
            - report.legal_deposit_amount
            - report.saving_amount
            + report.saving_withdrawal_amount
            - report.short_term_advance_amount
            AS supplied_amount,
        report.legal_deposit_amount
            + report.short_term_advance_amount
            + report.dormitory_fee_amount
            + report.mobile_rental_amount
            + report.wifi_fee_amount
            + report.other_deduction_amount
            AS deduction_amount,
        report.gross_payment_amount
            - report.legal_deposit_amount
            - report.saving_amount
            + report.saving_withdrawal_amount
            - report.short_term_advance_amount
            - report.dormitory_fee_amount
            - report.mobile_rental_amount
            - report.wifi_fee_amount
            - report.long_term_loan_repayment_amount
            - report.other_deduction_amount
            AS payment_amount
    FROM report_summary report
), detail AS (
    SELECT
        report.tenant_id,
        report.target_date,
        report.employee_id,
        employee.employee_code,
        employee.employee_name,
        COALESCE(contract.payment_cycle, 'MONTHLY') AS payment_cycle,
        CASE COALESCE(contract.payment_cycle, 'MONTHLY')
            WHEN 'DAILY' THEN 1
            WHEN 'WEEKLY' THEN 2
            WHEN 'MONTHLY' THEN 3
            ELSE 9
        END AS payment_cycle_order,
        report.gross_payment_amount,
        report.allowance_amount,
        report.legal_deposit_amount,
        report.saving_amount,
        report.saving_withdrawal_amount,
        report.short_term_advance_amount,
        report.supplied_amount,
        report.dormitory_fee_amount,
        report.mobile_rental_amount,
        report.wifi_fee_amount,
        report.long_term_loan_repayment_amount,
        report.other_deduction_amount,
        report.deduction_amount,
        report.payment_amount,
        report.payment_amount AS net_payment_amount
    FROM calculated report
    JOIN employee
      ON employee.tenant_id = report.tenant_id
     AND employee.id = report.employee_id
     AND employee.deleted_at IS NULL
    LEFT JOIN employee_contract contract
     ON contract.tenant_id = report.tenant_id
     AND contract.employee_id = report.employee_id
     AND contract.deleted_at IS NULL
), summarized AS (
    SELECT
        detail.*,
        ROW_NUMBER() OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
            ORDER BY detail.employee_code
        ) AS payment_cycle_row_number,
        COUNT(*) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS payment_cycle_row_count,
        SUM(detail.gross_payment_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_gross_payment_amount,
        SUM(detail.legal_deposit_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_legal_deposit_amount,
        SUM(detail.saving_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_saving_amount,
        SUM(detail.saving_withdrawal_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_saving_withdrawal_amount,
        SUM(detail.short_term_advance_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_short_term_advance_amount,
        SUM(detail.supplied_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_supplied_amount,
        SUM(detail.dormitory_fee_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_dormitory_fee_amount,
        SUM(detail.mobile_rental_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_mobile_rental_amount,
        SUM(detail.wifi_fee_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_wifi_fee_amount,
        SUM(detail.long_term_loan_repayment_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_long_term_loan_repayment_amount,
        SUM(detail.other_deduction_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_other_deduction_amount,
        SUM(detail.payment_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_payment_amount,
        SUM(detail.gross_payment_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_gross_payment_amount,
        SUM(detail.legal_deposit_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_legal_deposit_amount,
        SUM(detail.saving_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_saving_amount,
        SUM(detail.saving_withdrawal_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_saving_withdrawal_amount,
        SUM(detail.short_term_advance_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_short_term_advance_amount,
        SUM(detail.supplied_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_supplied_amount,
        SUM(detail.dormitory_fee_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_dormitory_fee_amount,
        SUM(detail.mobile_rental_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_mobile_rental_amount,
        SUM(detail.wifi_fee_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_wifi_fee_amount,
        SUM(detail.long_term_loan_repayment_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_long_term_loan_repayment_amount,
        SUM(detail.other_deduction_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_other_deduction_amount,
        SUM(detail.payment_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date,
                detail.payment_cycle
        ) AS cycle_payment_amount
    FROM detail
), denomination AS (
    SELECT
        summarized.*,
        summarized.total_payment_amount AS total_net_payment_amount,
        summarized.total_payment_amount AS bank_withdrawal_amount,
        GREATEST(FLOOR(summarized.payment_amount), 0) AS cash_payment_amount
    FROM summarized
), totals AS (
    SELECT
        denomination.*,
        SUM(FLOOR(denomination.cash_payment_amount / 10000)) OVER (
            PARTITION BY denomination.tenant_id, denomination.target_date
        ) AS bill_10000,
        SUM(FLOOR(MOD(denomination.cash_payment_amount, 10000) / 5000)) OVER (
            PARTITION BY denomination.tenant_id, denomination.target_date
        ) AS bill_5000,
        SUM(FLOOR(MOD(denomination.cash_payment_amount, 5000) / 1000)) OVER (
            PARTITION BY denomination.tenant_id, denomination.target_date
        ) AS bill_1000,
        SUM(FLOOR(MOD(denomination.cash_payment_amount, 1000) / 500)) OVER (
            PARTITION BY denomination.tenant_id, denomination.target_date
        ) AS coin_500,
        SUM(FLOOR(MOD(denomination.cash_payment_amount, 500) / 100)) OVER (
            PARTITION BY denomination.tenant_id, denomination.target_date
        ) AS coin_100,
        SUM(FLOOR(MOD(denomination.cash_payment_amount, 100) / 50)) OVER (
            PARTITION BY denomination.tenant_id, denomination.target_date
        ) AS coin_50,
        SUM(FLOOR(MOD(denomination.cash_payment_amount, 50) / 10)) OVER (
            PARTITION BY denomination.tenant_id, denomination.target_date
        ) AS coin_10,
        SUM(FLOOR(MOD(denomination.cash_payment_amount, 10) / 5)) OVER (
            PARTITION BY denomination.tenant_id, denomination.target_date
        ) AS coin_5,
        SUM(MOD(denomination.cash_payment_amount, 5)) OVER (
            PARTITION BY denomination.tenant_id, denomination.target_date
        ) AS coin_1
    FROM denomination
)
SELECT
    totals.*,
    DATE_FORMAT(totals.target_date, '%Y年%m月%d日')
        AS payment_date_label
FROM totals;

-- -----------------------------------------------------
-- プレビュー定義
-- -----------------------------------------------------
SET @tenant_id = 'default';
SET @now = NOW(6);

DELETE preview_column
FROM operation_report_preview_column preview_column
JOIN operation_report_preview preview
  ON preview.id = preview_column.operation_report_preview_id
WHERE preview.tenant_id = @tenant_id
  AND preview.report_code IN (
      'DAILY_LABOR_COST_PREVIEW',
      'DAILY_PAYMENT_PREPARATION'
  );

DELETE FROM operation_report_preview
WHERE tenant_id = @tenant_id
  AND report_code IN (
      'DAILY_LABOR_COST_PREVIEW',
      'DAILY_PAYMENT_PREPARATION'
  );

INSERT INTO operation_report_preview (
    tenant_id, created_at, updated_at,
    operation_type, report_code, report_name, job_code,
    table_name, filter_column_name,
    template_name, html_template_key,
    html_template_version, html_template_hash,
    order_by, display_order, active_flag, output_type
) VALUES
(
    @tenant_id, @now, @now,
    'DAILY',
    'DAILY_LABOR_COST_PREVIEW',
    '日別労務費一覧',
    NULL,
    'vw_daily_labor_cost_preview',
    'target_date',
    'daily_labor_cost.html',
    'documents/templates/reports/html/DAILY_LABOR_COST_PREVIEW/v1/template.html',
    1,
    NULL,
    'payment_cycle, employee_code',
    10,
    FALSE,
    'HTML_PREVIEW'
),
(
    @tenant_id, @now, @now,
    'DAILY',
    'DAILY_PAYMENT_PREPARATION',
    '給与支払表',
    NULL,
    'vw_daily_payment_preparation_preview',
    'target_date',
    'daily_payment_preparation.html',
    'documents/templates/reports/html/DAILY_PAYMENT_PREPARATION/v1/template.html',
    1,
    NULL,
    'payment_cycle_order, employee_code',
    20,
    TRUE,
    'HTML_PRINT'
);
