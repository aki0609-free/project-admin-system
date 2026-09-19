-- 封筒宛名印刷 V1（長形3号・角形2号）
-- 現行の帳票基盤、ローカルDocker、Testcontainers、AWSで共通利用する。

SET NAMES utf8mb4;
SET @tenant_id = 'default';
SET @now = NOW(6);

CREATE TABLE IF NOT EXISTS envelope_print_input (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    execution_id VARCHAR(100) NOT NULL,
    customer_id BIGINT NOT NULL,
    stamp VARCHAR(100) NULL,
    honorific VARCHAR(50) NULL,
    envelope_type VARCHAR(30) NULL,
    INDEX idx_envelope_print_input_execution (execution_id),
    INDEX idx_envelope_print_input_customer (tenant_id, customer_id)
);

CREATE TABLE IF NOT EXISTS envelope_print_output (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    execution_id VARCHAR(100) NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    postal_code VARCHAR(255) NULL,
    address VARCHAR(500) NULL,
    stamp VARCHAR(100) NULL,
    honorific VARCHAR(50) NULL,
    envelope_type VARCHAR(30) NULL,
    INDEX idx_envelope_print_output_execution (execution_id),
    INDEX idx_envelope_print_output_customer (tenant_id, customer_id)
);

DELETE param
FROM report_param param
JOIN report_master master ON master.id = param.report_master_id
WHERE master.tenant_id = @tenant_id
  AND master.report_code IN ('ENVELOPE_NAGA3', 'ENVELOPE_KAKU2');

INSERT INTO report_master (
    tenant_id, created_at, updated_at, deleted_at,
    report_code, report_name, template_file_name,
    work_table, input_table, output_table,
    source_view_name, history_table,
    pre_process_type, pre_process_sql, procedure_name, query_sql,
    cleanup_type, cleanup_sql, cleanup_procedure_name,
    layout_type, layout_count, file_name, output_format,
    use_signature, preview_enabled, active_flag
)
SELECT
    @tenant_id, @now, @now, NULL,
    definition.report_code,
    definition.report_name,
    definition.template_file_name,
    'envelope_print',
    'envelope_print_input',
    'envelope_print_output',
    NULL,
    NULL,
    'SQL',
    'INSERT INTO envelope_print_output (
        tenant_id, created_at, updated_at, execution_id,
        customer_id, customer_name, postal_code, address,
        stamp, honorific, envelope_type
     )
     SELECT
        input.tenant_id, NOW(6), NOW(6), input.execution_id,
        customer.id, customer.name, customer.post_no, customer.address,
        COALESCE(NULLIF(TRIM(input.stamp), ''''), ''請求書在中''),
        COALESCE(NULLIF(TRIM(input.honorific), ''''), ''御中''),
        input.envelope_type
     FROM envelope_print_input input
     JOIN customers customer
       ON customer.id = input.customer_id
      AND customer.tenant_id = input.tenant_id
      AND customer.deleted_at IS NULL
     WHERE input.execution_id = :executionId',
    NULL,
    'SELECT
        customer_name AS customerName,
        postal_code AS postalCode,
        address AS address,
        stamp AS stamp,
        honorific AS honorific,
        envelope_type AS envelopeType
     FROM envelope_print_output
     WHERE execution_id = :executionId
     ORDER BY id',
    'SQL',
    'DELETE output_row, input_row
     FROM envelope_print_input input_row
     LEFT JOIN envelope_print_output output_row
       ON output_row.execution_id = input_row.execution_id
     WHERE input_row.execution_id = :executionId',
    NULL,
    'SINGLE',
    1,
    definition.file_name,
    'PDF',
    FALSE,
    TRUE,
    TRUE
FROM (
    SELECT
        'ENVELOPE_NAGA3' report_code,
        '封筒印刷 長形3号' report_name,
        'envelope_naga3.jrxml' template_file_name,
        'envelope_naga3' file_name
    UNION ALL
    SELECT
        'ENVELOPE_KAKU2',
        '封筒印刷 角形2号',
        'envelope_kaku2.jrxml',
        'envelope_kaku2'
) definition
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at),
    deleted_at = NULL,
    report_name = VALUES(report_name),
    template_file_name = VALUES(template_file_name),
    work_table = VALUES(work_table),
    input_table = VALUES(input_table),
    output_table = VALUES(output_table),
    source_view_name = VALUES(source_view_name),
    history_table = VALUES(history_table),
    pre_process_type = VALUES(pre_process_type),
    pre_process_sql = VALUES(pre_process_sql),
    procedure_name = VALUES(procedure_name),
    query_sql = VALUES(query_sql),
    cleanup_type = VALUES(cleanup_type),
    cleanup_sql = VALUES(cleanup_sql),
    cleanup_procedure_name = VALUES(cleanup_procedure_name),
    layout_type = VALUES(layout_type),
    layout_count = VALUES(layout_count),
    file_name = VALUES(file_name),
    output_format = VALUES(output_format),
    use_signature = VALUES(use_signature),
    preview_enabled = VALUES(preview_enabled),
    active_flag = VALUES(active_flag);

INSERT INTO report_param (
    tenant_id, created_at, updated_at, deleted_at,
    report_master_id, param_name, param_label,
    param_type, control_type,
    required_flag, visible_flag, multiple_flag, filter_flag,
    default_value, placeholder, input_column_name,
    display_order, active_flag
)
SELECT
    @tenant_id, @now, @now, NULL,
    master.id,
    definition.param_name,
    definition.param_label,
    definition.param_type,
    definition.control_type,
    definition.required_flag,
    TRUE,
    definition.multiple_flag,
    TRUE,
    definition.default_value,
    definition.placeholder,
    definition.input_column_name,
    definition.display_order,
    TRUE
FROM report_master master
CROSS JOIN (
    SELECT
        'customerIds' param_name, '印刷する企業' param_label,
        'LONG' param_type, 'MULTI_SELECT' control_type,
        TRUE required_flag, TRUE multiple_flag,
        NULL default_value, '企業を選択' placeholder,
        'customer_id' input_column_name, 1 display_order
    UNION ALL
    SELECT
        'stamp', '封筒スタンプ', 'STRING', 'SELECT',
        TRUE, FALSE, '請求書在中', NULL, 'stamp', 2
    UNION ALL
    SELECT
        'honorific', '敬称', 'STRING', 'SELECT',
        TRUE, FALSE, '御中', NULL, 'honorific', 3
    UNION ALL
    SELECT
        'envelopeType', '封筒タイプ', 'STRING', 'SELECT',
        TRUE, FALSE, NULL, NULL, 'envelope_type', 4
) definition
WHERE master.tenant_id = @tenant_id
  AND master.report_code IN ('ENVELOPE_NAGA3', 'ENVELOPE_KAKU2');

INSERT INTO batch_job_definition (
    tenant_id, created_at, updated_at, deleted_at,
    job_code, job_name, job_type, target_code,
    immediate_executable, schedule_enabled, schedule_type,
    cron_expression, last_executed_at, next_execute_at,
    active_flag, description
)
VALUES
(
    @tenant_id, @now, @now, NULL,
    'PRINT_ENVELOPE_NAGA3', '封筒印刷 長形3号', 'REPORT', 'ENVELOPE_NAGA3',
    TRUE, FALSE, 'NONE', NULL, NULL, NULL, TRUE,
    '長形3号封筒の宛名PDFを生成します。'
),
(
    @tenant_id, @now, @now, NULL,
    'PRINT_ENVELOPE_KAKU2', '封筒印刷 角形2号', 'REPORT', 'ENVELOPE_KAKU2',
    TRUE, FALSE, 'NONE', NULL, NULL, NULL, TRUE,
    '角形2号封筒の宛名PDFを生成します。'
)
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at),
    deleted_at = NULL,
    job_name = VALUES(job_name),
    job_type = VALUES(job_type),
    target_code = VALUES(target_code),
    immediate_executable = VALUES(immediate_executable),
    schedule_enabled = VALUES(schedule_enabled),
    schedule_type = VALUES(schedule_type),
    cron_expression = VALUES(cron_expression),
    active_flag = VALUES(active_flag),
    description = VALUES(description);
