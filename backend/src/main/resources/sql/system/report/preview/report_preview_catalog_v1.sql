-- ProjectAdminSystem V1
-- 台帳を除く日次・月次帳票のプレビュー／バッチ登録
-- MySQL 8.x

SET NAMES utf8mb4;
SET @tenant_id = 'default';
SET @now = NOW(6);

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE operation_report_preview ADD COLUMN target_param_name VARCHAR(100) NULL AFTER filter_column_name',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_report_preview'
      AND column_name = 'target_param_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE operation_report_preview
    MODIFY COLUMN output_type VARCHAR(30) NOT NULL;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_operation_preview AS
SELECT
    source.tenant_id,
    DATE_FORMAT(source.target_month, '%Y-%m') AS target_month,
    source.period_from,
    source.period_to,
    source.employee_id,
    source.employee_code,
    source.employee_name,
    source.company_name,
    source.work_day_count,
    source.work_hours,
    source.overtime_hours,
    source.night_work_hours,
    source.holiday_work_hours,
    source.paid_leave_days,
    source.basic_salary,
    source.allowance_total,
    source.gross_amount,
    source.deduction_total,
    source.advance_payment_amount,
    source.net_payment_amount
FROM vw_monthly_pay_slip_latest source;

CREATE OR REPLACE VIEW vw_monthly_invoice_operation_preview AS
SELECT
    detail.tenant_id,
    DATE_FORMAT(detail.work_date, '%Y-%m') AS target_month,
    detail.customer_id,
    MAX(customer.name) AS customer_name,
    detail.customer_site_id,
    MAX(detail.site_name) AS site_name,
    detail.job_code,
    MAX(detail.job_name) AS job_name,
    detail.billing_unit,
    SUM(detail.base_quantity) AS base_quantity,
    MAX(detail.base_unit_price) AS base_unit_price,
    SUM(detail.base_amount) AS base_amount,
    SUM(detail.overtime_amount) AS overtime_amount,
    SUM(detail.night_amount) AS night_amount,
    SUM(detail.holiday_amount) AS holiday_amount,
    SUM(detail.commute_amount) AS commute_amount,
    SUM(
        detail.base_amount
        + detail.overtime_amount
        + detail.night_amount
        + detail.holiday_amount
        + detail.commute_amount
    ) AS line_amount,
    MIN(detail.calculation_ready_flag) AS calculation_ready_flag
FROM vw_monthly_invoice_latest_detail detail
LEFT JOIN customers customer
  ON customer.tenant_id = detail.tenant_id
 AND customer.id = detail.customer_id
 AND customer.deleted_at IS NULL
GROUP BY
    detail.tenant_id,
    DATE_FORMAT(detail.work_date, '%Y-%m'),
    detail.customer_id,
    detail.customer_site_id,
    detail.job_code,
    detail.billing_unit;

INSERT INTO batch_job_definition (
    tenant_id, job_code, job_name, job_type, target_code,
    immediate_executable, schedule_enabled, schedule_type,
    cron_expression, active_flag, description,
    created_at, updated_at
) VALUES
(
    @tenant_id, 'PRINT_DAILY_PAY_SLIP', '日払い明細出力',
    'REPORT', 'DAILY_PAY_SLIP',
    TRUE, FALSE, 'NONE', NULL, TRUE,
    '日次支払日に対する全従業員の日払い明細PDFを生成する',
    @now, @now
),
(
    @tenant_id, 'PRINT_MONTHLY_PAY_SLIP', '月給料明細出力',
    'REPORT', 'MONTHLY_PAY_SLIP',
    TRUE, FALSE, 'NONE', NULL, TRUE,
    '月次締めVersionから全従業員の月給料明細PDFを生成する',
    @now, @now
),
(
    @tenant_id, 'PRINT_MONTHLY_INVOICE', '月次請求書出力',
    'REPORT', 'MONTHLY_INVOICE',
    TRUE, FALSE, 'NONE', NULL, TRUE,
    '月次締め時に顧客マスターの請求書パターンを解決してPDFを生成する',
    @now, @now
)
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name),
    job_type = VALUES(job_type),
    target_code = VALUES(target_code),
    immediate_executable = VALUES(immediate_executable),
    schedule_enabled = VALUES(schedule_enabled),
    schedule_type = VALUES(schedule_type),
    active_flag = VALUES(active_flag),
    description = VALUES(description),
    updated_at = VALUES(updated_at);

INSERT INTO operation_report_preview (
    tenant_id, created_at, updated_at,
    operation_type, report_code, report_name, job_code,
    table_name, filter_column_name, target_param_name,
    template_name, html_template_key, html_template_version,
    order_by, display_order, active_flag, output_type
) VALUES
(
    @tenant_id, @now, @now,
    'DAILY', 'DAILY_PAY_SLIP', '日払い明細',
    'PRINT_DAILY_PAY_SLIP',
    'vw_daily_pay_slip_latest', 'payment_date', 'paymentDate',
    'daily_pay_slip.jrxml',
    'documents/templates/reports/html/DAILY_PAY_SLIP/v2/template.html', 2,
    'employee_code', 30, TRUE, 'PDF'
),
(
    @tenant_id, @now, @now,
    'MONTHLY', 'MONTHLY_PAY_SLIP', '月給料明細',
    'PRINT_MONTHLY_PAY_SLIP',
    'vw_monthly_pay_slip_operation_preview', 'target_month', 'targetMonth',
    'monthly_pay_slip.jrxml',
    'documents/templates/reports/html/MONTHLY_PAY_SLIP/v1/template.html', 1,
    'employee_code', 10, TRUE, 'PDF'
),
(
    @tenant_id, @now, @now,
    'MONTHLY', 'MONTHLY_INVOICE', '請求書',
    'PRINT_MONTHLY_INVOICE',
    'vw_monthly_invoice_operation_preview', 'target_month', 'targetMonth',
    'monthly_invoice.html', NULL, 1,
    'customer_name, site_name, job_code', 20, TRUE, 'PDF'
)
ON DUPLICATE KEY UPDATE
    report_name = VALUES(report_name),
    job_code = VALUES(job_code),
    table_name = VALUES(table_name),
    filter_column_name = VALUES(filter_column_name),
    target_param_name = VALUES(target_param_name),
    template_name = VALUES(template_name),
    html_template_key = VALUES(html_template_key),
    html_template_version = VALUES(html_template_version),
    order_by = VALUES(order_by),
    display_order = VALUES(display_order),
    active_flag = VALUES(active_flag),
    output_type = VALUES(output_type),
    updated_at = VALUES(updated_at);

DELETE preview_column
FROM operation_report_preview_column preview_column
JOIN operation_report_preview preview
  ON preview.id = preview_column.operation_report_preview_id
WHERE preview.tenant_id = @tenant_id
  AND preview.report_code IN ('MONTHLY_INVOICE');

SET @monthly_invoice_preview_id = (
    SELECT id FROM operation_report_preview
    WHERE tenant_id = @tenant_id
      AND operation_type = 'MONTHLY'
      AND report_code = 'MONTHLY_INVOICE'
    LIMIT 1
);

INSERT INTO operation_report_preview_column (
    tenant_id, created_at, updated_at,
    operation_report_preview_id,
    preview_name, column_name, display_order, active_flag
) VALUES
(@tenant_id, @now, @now, @monthly_invoice_preview_id, '顧客名', 'customer_name', 1, TRUE),
(@tenant_id, @now, @now, @monthly_invoice_preview_id, '現場名', 'site_name', 2, TRUE),
(@tenant_id, @now, @now, @monthly_invoice_preview_id, '職種', 'job_name', 3, TRUE),
(@tenant_id, @now, @now, @monthly_invoice_preview_id, '請求単位', 'billing_unit', 4, TRUE),
(@tenant_id, @now, @now, @monthly_invoice_preview_id, '数量', 'base_quantity', 5, TRUE),
(@tenant_id, @now, @now, @monthly_invoice_preview_id, '基本金額', 'base_amount', 6, TRUE),
(@tenant_id, @now, @now, @monthly_invoice_preview_id, '割増金額', 'overtime_amount', 7, TRUE),
(@tenant_id, @now, @now, @monthly_invoice_preview_id, '行合計', 'line_amount', 8, TRUE),
(@tenant_id, @now, @now, @monthly_invoice_preview_id, '計算可否', 'calculation_ready_flag', 9, TRUE);
