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
    source.target_month,
    DATE_FORMAT(source.target_month, '%Y年%c月') AS target_month_label,
    source.period_from,
    source.period_to,
    DATE_FORMAT(source.period_from, '%Y年%c月%e日') AS period_start_label,
    DATE_FORMAT(source.period_to, '%Y年%c月%e日') AS period_end_label,
    COALESCE(closing.closing_version, 0) AS closing_version,
    CASE payroll.payment_day_type
        WHEN 'END_OF_MONTH' THEN DATE_FORMAT(
            LAST_DAY(DATE_ADD(
                source.target_month,
                INTERVAL COALESCE(payroll.payment_month_offset, 0) MONTH
            )),
            '%Y年%c月%e日'
        )
        WHEN 'DAY_OF_MONTH' THEN DATE_FORMAT(
            DATE_ADD(
                DATE_FORMAT(
                    DATE_ADD(
                        source.target_month,
                        INTERVAL COALESCE(payroll.payment_month_offset, 0) MONTH
                    ),
                    '%Y-%m-01'
                ),
                INTERVAL (
                    LEAST(
                        payroll.payment_day_value,
                        DAY(LAST_DAY(DATE_ADD(
                            source.target_month,
                            INTERVAL COALESCE(payroll.payment_month_offset, 0) MONTH
                        )))
                    ) - 1
                ) DAY
            ),
            '%Y年%c月%e日'
        )
        ELSE NULL
    END AS payment_date_label,
    source.employee_id,
    source.employee_code,
    source.employee_name,
    source.company_name,
    source.work_day_count,
    source.work_hours,
    source.work_hours AS total_work_hours,
    CONCAT(
        FLOOR(ROUND(source.work_hours * 60) / 60),
        '時間',
        MOD(ROUND(source.work_hours * 60), 60),
        '分'
    ) AS work_hours_label,
    source.overtime_hours,
    source.overtime_hours AS total_overtime_hours,
    CONCAT(
        FLOOR(ROUND(source.overtime_hours * 60) / 60),
        '時間',
        MOD(ROUND(source.overtime_hours * 60), 60),
        '分'
    ) AS overtime_hours_label,
    source.night_work_hours,
    source.night_work_hours AS total_night_work_hours,
    CONCAT(
        FLOOR(ROUND(source.night_work_hours * 60) / 60),
        '時間',
        MOD(ROUND(source.night_work_hours * 60), 60),
        '分'
    ) AS night_work_hours_label,
    source.holiday_work_hours,
    CONCAT(
        FLOOR(ROUND(source.holiday_work_hours * 60) / 60),
        '時間',
        MOD(ROUND(source.holiday_work_hours * 60), 60),
        '分'
    ) AS holiday_work_hours_label,
    source.paid_leave_days,
    source.basic_salary,
    source.allowance_total,
    source.gross_amount,
    source.health_insurance,
    source.child_care_contribution,
    source.pension_insurance,
    source.employment_insurance,
    source.income_tax,
    source.resident_tax,
    source.taxable_amount,
    source.legal_deduction_total AS tax_deduction_total,
    source.other_deduction_total AS other_deduction_total,
    source.deduction_total,
    source.advance_payment_amount,
    source.loan_balance,
    source.saving_balance,
    source.legal_deposit_refund_amount,
    source.net_payment_amount,
    source.net_payment_amount AS net_amount,
    source.allowance_item_name_01 AS allowance_item_name_1,
    source.allowance_item_value_01 AS allowance_item_value_1,
    source.allowance_item_name_02 AS allowance_item_name_2,
    source.allowance_item_value_02 AS allowance_item_value_2,
    source.allowance_item_name_03 AS allowance_item_name_3,
    source.allowance_item_value_03 AS allowance_item_value_3,
    source.allowance_item_name_04 AS allowance_item_name_4,
    source.allowance_item_value_04 AS allowance_item_value_4,
    source.allowance_item_name_05 AS allowance_item_name_5,
    source.allowance_item_value_05 AS allowance_item_value_5,
    source.allowance_item_name_06 AS allowance_item_name_6,
    source.allowance_item_value_06 AS allowance_item_value_6,
    source.allowance_item_name_07 AS allowance_item_name_7,
    source.allowance_item_value_07 AS allowance_item_value_7,
    source.allowance_item_name_08 AS allowance_item_name_8,
    source.allowance_item_value_08 AS allowance_item_value_8,
    source.allowance_item_name_09 AS allowance_item_name_9,
    source.allowance_item_value_09 AS allowance_item_value_9,
    source.allowance_item_name_10 AS allowance_item_name_10,
    source.allowance_item_value_10 AS allowance_item_value_10,
    source.allowance_item_name_11 AS allowance_item_name_11,
    source.allowance_item_value_11 AS allowance_item_value_11,
    source.allowance_item_name_12 AS allowance_item_name_12,
    source.allowance_item_value_12 AS allowance_item_value_12,
    source.legal_item_name_01 AS tax_deduction_item_name_1,
    source.legal_item_value_01 AS tax_deduction_item_value_1,
    source.legal_item_name_02 AS tax_deduction_item_name_2,
    source.legal_item_value_02 AS tax_deduction_item_value_2,
    source.legal_item_name_03 AS tax_deduction_item_name_3,
    source.legal_item_value_03 AS tax_deduction_item_value_3,
    source.legal_item_name_04 AS tax_deduction_item_name_4,
    source.legal_item_value_04 AS tax_deduction_item_value_4,
    source.legal_item_name_05 AS tax_deduction_item_name_5,
    source.legal_item_value_05 AS tax_deduction_item_value_5,
    source.legal_item_name_06 AS tax_deduction_item_name_6,
    source.legal_item_value_06 AS tax_deduction_item_value_6,
    source.legal_item_name_07 AS tax_deduction_item_name_7,
    source.legal_item_value_07 AS tax_deduction_item_value_7,
    source.legal_item_name_08 AS tax_deduction_item_name_8,
    source.legal_item_value_08 AS tax_deduction_item_value_8,
    source.legal_item_name_09 AS tax_deduction_item_name_9,
    source.legal_item_value_09 AS tax_deduction_item_value_9,
    source.legal_item_name_10 AS tax_deduction_item_name_10,
    source.legal_item_value_10 AS tax_deduction_item_value_10,
    source.legal_item_name_11 AS tax_deduction_item_name_11,
    source.legal_item_value_11 AS tax_deduction_item_value_11,
    source.legal_item_name_12 AS tax_deduction_item_name_12,
    source.legal_item_value_12 AS tax_deduction_item_value_12,
    source.other_item_name_01 AS deduction_item_name_1,
    source.other_item_value_01 AS deduction_item_value_1,
    source.other_item_name_02 AS deduction_item_name_2,
    source.other_item_value_02 AS deduction_item_value_2,
    source.other_item_name_03 AS deduction_item_name_3,
    source.other_item_value_03 AS deduction_item_value_3,
    source.other_item_name_04 AS deduction_item_name_4,
    source.other_item_value_04 AS deduction_item_value_4,
    source.other_item_name_05 AS deduction_item_name_5,
    source.other_item_value_05 AS deduction_item_value_5,
    source.other_item_name_06 AS deduction_item_name_6,
    source.other_item_value_06 AS deduction_item_value_6,
    source.other_item_name_07 AS deduction_item_name_7,
    source.other_item_value_07 AS deduction_item_value_7,
    source.other_item_name_08 AS deduction_item_name_8,
    source.other_item_value_08 AS deduction_item_value_8,
    source.other_item_name_09 AS deduction_item_name_9,
    source.other_item_value_09 AS deduction_item_value_9,
    source.other_item_name_10 AS deduction_item_name_10,
    source.other_item_value_10 AS deduction_item_value_10,
    source.other_item_name_11 AS deduction_item_name_11,
    source.other_item_value_11 AS deduction_item_value_11,
    source.other_item_name_12 AS deduction_item_name_12,
    source.other_item_value_12 AS deduction_item_value_12
FROM vw_monthly_pay_slip_latest source
LEFT JOIN monthly_closings closing
  ON closing.tenant_id = source.tenant_id
 AND closing.target_month = source.target_month
 AND closing.deleted_at IS NULL
LEFT JOIN (
    SELECT setting.*
    FROM closing_setting setting
    JOIN (
        SELECT tenant_id, MAX(id) AS setting_id
        FROM closing_setting
        WHERE setting_code = 'PAYROLL'
          AND active_flag = TRUE
          AND deleted_at IS NULL
        GROUP BY tenant_id
    ) selected
      ON selected.setting_id = setting.id
     AND selected.tenant_id = setting.tenant_id
) payroll
  ON payroll.tenant_id = source.tenant_id;

CREATE OR REPLACE VIEW vw_monthly_invoice_operation_preview AS
SELECT
    detail.tenant_id,
    DATE_FORMAT(detail.work_date, '%Y-%m') AS target_month,
    detail.work_date,
    detail.customer_id,
    MAX(customer.name) AS customer_name,
    MAX(COALESCE(customer.invoice_type, 'PATTERN_1')) AS customer_invoice_type,
    CURRENT_DATE AS invoice_date,
    'プレビュー' AS invoice_number,
    MIN(detail.work_date) OVER (
        PARTITION BY detail.tenant_id,
            DATE_FORMAT(detail.work_date, '%Y-%m'), detail.customer_id
    ) AS period_start,
    MAX(detail.work_date) OVER (
        PARTITION BY detail.tenant_id,
            DATE_FORMAT(detail.work_date, '%Y-%m'), detail.customer_id
    ) AS period_end,
    MAX(company.company_name) AS company_name,
    MAX(company.postal_code) AS company_postal_code,
    MAX(CONCAT_WS('', company.prefecture, company.city,
        company.address_line1, company.address_line2)) AS company_address,
    MAX(company.phone) AS company_phone,
    MAX(company.fax) AS company_fax,
    MAX(company.qualified_invoice_issuer_number)
        AS qualified_invoice_issuer_number,
    MAX(CONCAT_WS(' ', company.invoice_bank_name,
        company.invoice_bank_branch_name, company.invoice_bank_account_type,
        company.invoice_bank_account_number,
        company.invoice_bank_account_holder)) AS bank_display_text,
    MAX(company.invoice_note) AS invoice_note,
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
    SUM(detail.other_amount) AS other_amount,
    SUM(
        detail.base_amount
        + detail.overtime_amount
        + detail.night_amount
        + detail.holiday_amount
        + detail.commute_amount
        + detail.other_amount
    ) AS line_amount,
    SUM(SUM(
        detail.base_amount
        + detail.overtime_amount
        + detail.night_amount
        + detail.holiday_amount
        + detail.commute_amount
        + detail.other_amount
    )) OVER (
        PARTITION BY detail.tenant_id,
            DATE_FORMAT(detail.work_date, '%Y-%m'), detail.customer_id
    ) AS subtotal_amount,
    ROUND(SUM(SUM(
        detail.base_amount
        + detail.overtime_amount
        + detail.night_amount
        + detail.holiday_amount
        + detail.commute_amount
        + detail.other_amount
    )) OVER (
        PARTITION BY detail.tenant_id,
            DATE_FORMAT(detail.work_date, '%Y-%m'), detail.customer_id
    ) * 0.10, 0) AS tax_amount,
    ROUND(SUM(SUM(
        detail.base_amount
        + detail.overtime_amount
        + detail.night_amount
        + detail.holiday_amount
        + detail.commute_amount
        + detail.other_amount
    )) OVER (
        PARTITION BY detail.tenant_id,
            DATE_FORMAT(detail.work_date, '%Y-%m'), detail.customer_id
    ) * 1.10, 0) AS total_amount,
    MIN(detail.calculation_ready_flag) AS calculation_ready_flag
FROM vw_monthly_invoice_latest_detail detail
LEFT JOIN customers customer
  ON customer.tenant_id = detail.tenant_id
 AND customer.id = detail.customer_id
 AND customer.deleted_at IS NULL
LEFT JOIN company_profile company
  ON company.tenant_id = detail.tenant_id
 AND company.active_flag = TRUE
 AND company.deleted_at IS NULL
 AND company.id = (
     SELECT MIN(selected.id)
     FROM company_profile selected
     WHERE selected.tenant_id = detail.tenant_id
       AND selected.active_flag = TRUE
       AND selected.deleted_at IS NULL
 )
GROUP BY
    detail.tenant_id,
    DATE_FORMAT(detail.work_date, '%Y-%m'),
    detail.work_date,
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
    @tenant_id, 'PRINT_DAILY_PAY_SLIP', '支払明細書出力',
    'REPORT', 'DAILY_PAY_SLIP',
    TRUE, FALSE, 'NONE', NULL, TRUE,
    '日次支払日に対する全従業員の支払明細書PDFを生成する',
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
    'DAILY', 'DAILY_PAY_SLIP', '支払明細書',
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
    'documents/templates/reports/html/MONTHLY_PAY_SLIP/v3/template.html', 3,
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
