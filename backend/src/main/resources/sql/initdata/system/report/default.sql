INSERT INTO report_master (
    tenant_id,
    report_code,
    report_name,
    template_file_name,
    work_table,
    input_table,
    output_table,
    pre_process_type,
    pre_process_sql,
    procedure_name,
    output_path,
    query_sql,
    cleanup_type,
    cleanup_sql,
    cleanup_procedure_name,
    layout_type,
    layout_count,
    file_name,
    output_format,
    use_signature,
    preview_enabled,
    active_flag,
    created_at,
    updated_at
) VALUES
(
    'default',
    'ENVELOPE_NAGA3',
    '封筒印刷 長形3号',
    'envelope_naga3.jrxml',
    'envelope_print',
    NULL,
    NULL,
    'NONE',
    NULL,
    NULL,
    '/reports/envelope',
    'SELECT
        id,
        customer_name AS customerName,
        postal_code AS postalCode,
        address AS address
     FROM customers
     WHERE id IN (:customerIds)',
    'NONE',
    NULL,
    NULL,
    'SINGLE',
    1,
    'envelope_naga3',
    'PDF',
    false,
    true,
    true,
    NOW(),
    NOW()
),
(
    'default',
    'ENVELOPE_KAKU2',
    '封筒印刷 角2',
    'envelope_kaku2.jrxml',
    'envelope_print',
    NULL,
    NULL,
    'NONE',
    NULL,
    NULL,
    '/reports/envelope',
    'SELECT
        id,
        customer_name AS customerName,
        postal_code AS postalCode,
        address AS address
     FROM customers
     WHERE id IN (:customerIds)',
    'NONE',
    NULL,
    NULL,
    'SINGLE',
    1,
    'envelope_kaku2',
    'PDF',
    false,
    true,
    true,
    NOW(),
    NOW()
);

SET @naga3_id = (
    SELECT id
    FROM report_master
    WHERE report_code = 'ENVELOPE_NAGA3'
    LIMIT 1
);

INSERT INTO report_param (
    tenant_id,
    report_master_id,
    param_name,
    param_label,
    param_type,
    control_type,
    required_flag,
    visible_flag,
    multiple_flag,
    filter_flag,
    default_value,
    placeholder,
    input_column_name,
    display_order,
    active_flag,
    created_at,
    updated_at
)
VALUES
(
    'default',
    @naga3_id,
    'customerIds',
    '顧客',
    'LONG',
    'SELECT',
    true,
    true,
    true,
    true,
    NULL,
    '顧客を選択',
    'customer_id',
    1,
    true,
    NOW(),
    NOW()
),
(
    'default',
    @naga3_id,
    'stamp',
    'スタンプ',
    'STRING',
    'TEXT',
    false,
    true,
    false,
    true,
    '請求書在中',
    '請求書在中',
    'stamp',
    2,
    true,
    NOW(),
    NOW()
),
(
    'default',
    @naga3_id,
    'honorific',
    '敬称',
    'STRING',
    'SELECT',
    true,
    true,
    false,
    true,
    '御中',
    NULL,
    'honorific',
    3,
    true,
    NOW(),
    NOW()
),
(
    'default',
    @naga3_id,
    'fontFamily',
    'フォント',
    'STRING',
    'SELECT',
    true,
    true,
    false,
    true,
    'Yu Gothic',
    NULL,
    'font_family',
    4,
    true,
    NOW(),
    NOW()
),
(
    'default',
    @naga3_id,
    'fontSize',
    'フォントサイズ',
    'LONG',
    'NUMBER',
    true,
    true,
    false,
    true,
    '16',
    NULL,
    'font_size',
    5,
    true,
    NOW(),
    NOW()
);

SET @kaku2_id = (
    SELECT id
    FROM report_master
    WHERE report_code = 'ENVELOPE_KAKU2'
    LIMIT 1
);

DELETE FROM report_param
WHERE report_master_id = @kaku2_id;

INSERT INTO report_param (
    tenant_id,
    report_master_id,
    param_name,
    param_label,
    param_type,
    control_type,
    required_flag,
    visible_flag,
    multiple_flag,
    filter_flag,
    default_value,
    placeholder,
    input_column_name,
    display_order,
    active_flag,
    created_at,
    updated_at
)
SELECT
    tenant_id,
    @kaku2_id,
    param_name,
    param_label,
    param_type,
    control_type,
    required_flag,
    visible_flag,
    multiple_flag,
    filter_flag,
    default_value,
    placeholder,
    input_column_name,
    display_order,
    active_flag,
    NOW(),
    NOW()
FROM report_param
WHERE report_master_id = @naga3_id
  AND deleted_at IS NULL
ORDER BY display_order;