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
-- work_date単位の最新承認済み日報を表示する。
-- -----------------------------------------------------
CREATE OR REPLACE VIEW vw_daily_labor_cost_preview AS
WITH labor AS (
    SELECT
        dr.tenant_id,
        dr.work_date AS target_date,
        dr.employee_id,
        employee.employee_code,
        employee.employee_name,
        COALESCE(contract.payment_cycle, 'MONTHLY') AS payment_cycle,
        COALESCE(SUM(dr.estimated_gross_pay_amount), 0)
            AS gross_payment_amount
    FROM daily_report dr
    JOIN employee
      ON employee.tenant_id = dr.tenant_id
     AND employee.id = dr.employee_id
     AND employee.deleted_at IS NULL
    LEFT JOIN employee_contract contract
      ON contract.tenant_id = dr.tenant_id
     AND contract.employee_id = dr.employee_id
     AND contract.deleted_at IS NULL
    WHERE dr.deleted_at IS NULL
      AND dr.approval_status = 'APPROVED'
    GROUP BY
        dr.tenant_id,
        dr.work_date,
        dr.employee_id,
        employee.employee_code,
        employee.employee_name,
        contract.payment_cycle
), detail AS (
    SELECT
        labor.*,
        COALESCE(payment.actual_amount, 0) AS payment_amount
    FROM labor
    LEFT JOIN daily_payments payment
      ON payment.tenant_id = labor.tenant_id
     AND payment.payment_date = labor.target_date
     AND payment.employee_id = labor.employee_id
     AND payment.status <> 'CANCELLED'
     AND payment.deleted_at IS NULL
)
SELECT
    detail.tenant_id,
    detail.target_date,
    DATE_FORMAT(detail.target_date, '%Y年%m月%d日') AS work_date_label,
    detail.employee_id,
    detail.employee_code,
    detail.employee_name,
    detail.payment_cycle,
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
-- payment_date単位に、日報計算値と確定したdaily_paymentsを統合する。
-- -----------------------------------------------------
CREATE OR REPLACE VIEW vw_daily_payment_preparation_preview AS
WITH report_summary AS (
    SELECT
        dr.tenant_id,
        dr.payment_date AS target_date,
        dr.employee_id,
        COALESCE(SUM(dr.estimated_gross_pay_amount), 0)
            AS gross_payment_amount,
        COALESCE(SUM(dr.allowance_amount), 0) AS allowance_amount,
        COALESCE(SUM(
            dr.deduction_amount
            + dr.saving_amount
            + dr.loan_repayment_amount
        ), 0) AS deduction_amount,
        COALESCE(SUM(dr.estimated_net_pay_amount), 0)
            AS estimated_net_payment_amount
    FROM daily_report dr
    WHERE dr.deleted_at IS NULL
      AND dr.approval_status = 'APPROVED'
      AND dr.payment_date IS NOT NULL
    GROUP BY dr.tenant_id, dr.payment_date, dr.employee_id
), payment_keys AS (
    SELECT tenant_id, target_date, employee_id
    FROM report_summary
    UNION
    SELECT tenant_id, payment_date, employee_id
    FROM daily_payments
    WHERE deleted_at IS NULL
      AND status <> 'CANCELLED'
), detail AS (
    SELECT
        payment_key.tenant_id,
        payment_key.target_date,
        payment_key.employee_id,
        COALESCE(payment.employee_code, employee.employee_code)
            AS employee_code,
        COALESCE(payment.employee_name, employee.employee_name)
            AS employee_name,
        COALESCE(contract.payment_cycle, 'MONTHLY') AS payment_cycle,
        COALESCE(
            report.gross_payment_amount,
            payment.planned_amount,
            0
        ) AS gross_payment_amount,
        COALESCE(report.allowance_amount, 0) AS allowance_amount,
        COALESCE(report.deduction_amount, 0) AS deduction_amount,
        CASE
            WHEN payment.id IS NOT NULL
                THEN COALESCE(payment.actual_amount, 0)
            ELSE COALESCE(report.estimated_net_payment_amount, 0)
        END AS net_payment_amount
    FROM payment_keys payment_key
    JOIN employee
      ON employee.tenant_id = payment_key.tenant_id
     AND employee.id = payment_key.employee_id
     AND employee.deleted_at IS NULL
    LEFT JOIN report_summary report
      ON report.tenant_id = payment_key.tenant_id
     AND report.target_date = payment_key.target_date
     AND report.employee_id = payment_key.employee_id
    LEFT JOIN daily_payments payment
      ON payment.tenant_id = payment_key.tenant_id
     AND payment.payment_date = payment_key.target_date
     AND payment.employee_id = payment_key.employee_id
     AND payment.status <> 'CANCELLED'
     AND payment.deleted_at IS NULL
    LEFT JOIN employee_contract contract
      ON contract.tenant_id = payment_key.tenant_id
     AND contract.employee_id = payment_key.employee_id
     AND contract.deleted_at IS NULL
), totals AS (
    SELECT
        detail.*,
        SUM(detail.net_payment_amount) OVER (
            PARTITION BY detail.tenant_id, detail.target_date
        ) AS total_net_payment_amount
    FROM detail
)
SELECT
    totals.*,
    DATE_FORMAT(totals.target_date, '%Y年%m月%d日')
        AS payment_date_label,
    FLOOR(GREATEST(totals.total_net_payment_amount, 0) / 10000)
        AS bill_10000,
    FLOOR(MOD(GREATEST(totals.total_net_payment_amount, 0), 10000) / 5000)
        AS bill_5000,
    FLOOR(MOD(GREATEST(totals.total_net_payment_amount, 0), 5000) / 1000)
        AS bill_1000,
    FLOOR(MOD(GREATEST(totals.total_net_payment_amount, 0), 1000) / 500)
        AS coin_500,
    FLOOR(MOD(GREATEST(totals.total_net_payment_amount, 0), 500) / 100)
        AS coin_100,
    FLOOR(MOD(GREATEST(totals.total_net_payment_amount, 0), 100) / 50)
        AS coin_50
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
    TRUE,
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
    'payment_cycle, employee_code',
    20,
    TRUE,
    'HTML_PRINT'
);
