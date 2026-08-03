-- 月次請求書 3パターン 帳票マスター V1
-- monthly_invoice_foundation_v1.sql / render_views_v1.sql 適用後に実行する。

SET NAMES utf8mb4;

SET @tenant_id = 'default';
SET @now = NOW(6);

DELETE param
FROM report_param param
JOIN report_master master ON master.id = param.report_master_id
WHERE master.tenant_id = @tenant_id
  AND master.report_code IN (
      'MONTHLY_INVOICE_PATTERN_1',
      'MONTHLY_INVOICE_PATTERN_2',
      'MONTHLY_INVOICE_PATTERN_3'
  );

DELETE FROM report_master
WHERE tenant_id = @tenant_id
  AND report_code IN (
      'MONTHLY_INVOICE_PATTERN_1',
      'MONTHLY_INVOICE_PATTERN_2',
      'MONTHLY_INVOICE_PATTERN_3'
  );

INSERT INTO report_master (
    tenant_id, created_at, updated_at,
    report_code, report_name, template_file_name,
    work_table, input_table, output_table,
    source_view_name, history_table,
    pre_process_type, pre_process_sql, procedure_name, query_sql,
    cleanup_type, cleanup_sql, cleanup_procedure_name,
    layout_type, layout_count, file_name, output_format,
    use_signature, preview_enabled, active_flag
)
SELECT
    @tenant_id,
    @now,
    @now,
    definition.report_code,
    definition.report_name,
    definition.template_file_name,
    'monthly_invoice',
    'monthly_invoice_input',
    'monthly_invoice_render_execution',
    'vw_monthly_invoice_latest_detail',
    'monthly_invoice_history',
    'PROCEDURE',
    NULL,
    'sp_monthly_invoice_snapshot',
    definition.query_sql,
    'PROCEDURE',
    NULL,
    'sp_monthly_invoice_cleanup',
    'SINGLE',
    1,
    '請求書_${targetMonth}_${customerId}_v${closingVersion}',
    'PDF',
    FALSE,
    TRUE,
    TRUE
FROM (
    SELECT
        'MONTHLY_INVOICE_PATTERN_1' AS report_code,
        '月次請求書（職種別）' AS report_name,
        'monthly_invoice_pattern_1.jrxml' AS template_file_name,
        'select * from vw_monthly_invoice_pattern_1_render
where execution_id = :executionId
order by work_date, job_code' AS query_sql
    UNION ALL
    SELECT
        'MONTHLY_INVOICE_PATTERN_2',
        '月次請求書（職種・役職別）',
        'monthly_invoice_pattern_2.jrxml',
        'select * from vw_monthly_invoice_pattern_2_render
where execution_id = :executionId
order by job_code, site_role_code, metric_order'
    UNION ALL
    SELECT
        'MONTHLY_INVOICE_PATTERN_3',
        '月次請求書（現場・職種・役職別）',
        'monthly_invoice_pattern_3.jrxml',
        'select * from vw_monthly_invoice_pattern_3_render
where execution_id = :executionId
order by customer_site_id, job_code, site_role_code, metric_order'
) definition;

INSERT INTO report_param (
    tenant_id, created_at, updated_at, report_master_id,
    param_name, param_label, param_type, control_type,
    required_flag, visible_flag, multiple_flag, filter_flag,
    default_value, placeholder, input_column_name,
    display_order, active_flag
)
SELECT
    @tenant_id,
    @now,
    @now,
    master.id,
    definition.param_name,
    definition.param_label,
    definition.param_type,
    definition.control_type,
    definition.required_flag,
    definition.visible_flag,
    FALSE,
    definition.filter_flag,
    definition.default_value,
    definition.placeholder,
    definition.input_column_name,
    definition.display_order,
    TRUE
FROM report_master master
CROSS JOIN (
    SELECT 'targetMonth' param_name, '対象月' param_label,
           'STRING' param_type, 'TEXT' control_type,
           TRUE required_flag, TRUE visible_flag, TRUE filter_flag,
           NULL default_value, 'YYYY-MM' placeholder,
           'target_month' input_column_name, 1 display_order
    UNION ALL
    SELECT 'periodFrom', '請求期間開始日', 'DATE', 'DATE',
           TRUE, FALSE, FALSE, NULL, NULL, 'period_from', 2
    UNION ALL
    SELECT 'periodTo', '請求期間終了日', 'DATE', 'DATE',
           TRUE, FALSE, FALSE, NULL, NULL, 'period_to', 3
    UNION ALL
    SELECT 'closingVersion', '締めVersion', 'LONG', 'NUMBER',
           TRUE, FALSE, FALSE, NULL, NULL, 'closing_version', 4
    UNION ALL
    SELECT 'executionMode', '実行モード', 'STRING', 'TEXT',
           TRUE, FALSE, FALSE, NULL, NULL, 'execution_mode', 5
    UNION ALL
    SELECT 'customerId', '顧客', 'LONG', 'TEXT',
           TRUE, TRUE, TRUE, NULL, '顧客を選択', 'customer_id', 6
    UNION ALL
    SELECT 'taxRate', '消費税率', 'DECIMAL', 'NUMBER',
           TRUE, FALSE, FALSE, '0.10', NULL, 'tax_rate', 7
) definition
WHERE master.tenant_id = @tenant_id
  AND master.report_code IN (
      'MONTHLY_INVOICE_PATTERN_1',
      'MONTHLY_INVOICE_PATTERN_2',
      'MONTHLY_INVOICE_PATTERN_3'
  );
