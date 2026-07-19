-- =========================================
-- import_export_target : 既存削除
-- =========================================
DELETE c
FROM import_export_column c
INNER JOIN import_export_target t
    ON t.id = c.target_id
WHERE t.target_code IN (
    'BACKUP_REPORT_MASTER',
    'BACKUP_REPORT_PARAM',
    'BACKUP_REPORT_SIGNATURE'
);

DELETE FROM import_export_target
WHERE target_code IN (
    'BACKUP_REPORT_MASTER',
    'BACKUP_REPORT_PARAM',
    'BACKUP_REPORT_SIGNATURE'
);

-- =========================================
-- import_export_target : report_master
-- =========================================
INSERT INTO import_export_target (
    target_code,
    target_name,
    table_name,
    description,
    backup_enabled,
    import_enabled,
    active_flag,
    tenant_id,
    created_at,
    updated_at
) VALUES (
    'BACKUP_REPORT_MASTER',
    '帳票マスタ',
    'report_master',
    '帳票マスタのバックアップ出力',
    true,
    false,
    true,
    'default',
    NOW(),
    NOW()
);

-- =========================================
-- import_export_column : report_master
-- =========================================
INSERT INTO import_export_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    required_flag,
    key_flag,
    export_flag,
    import_flag,
    order_no,
    tenant_id,
    created_at,
    updated_at
)
SELECT id, 'id', 'id', 'LONG', true, true, true, false, 1, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'report_code', 'report_code', 'STRING', true, true, true, false, 2, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'report_name', 'report_name', 'STRING', true, false, true, false, 3, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'template_file_name', 'template_file_name', 'STRING', false, false, true, false, 4, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'work_table', 'work_table', 'STRING', true, false, true, false, 5, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'input_table', 'input_table', 'STRING', false, false, true, false, 6, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'output_table', 'output_table', 'STRING', false, false, true, false, 7, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'pre_process_type', 'pre_process_type', 'STRING', true, false, true, false, 8, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'pre_process_sql', 'pre_process_sql', 'STRING', false, false, true, false, 9, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'procedure_name', 'procedure_name', 'STRING', false, false, true, false, 10, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'output_path', 'output_path', 'STRING', false, false, true, false, 11, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'query_sql', 'query_sql', 'STRING', true, false, true, false, 12, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'cleanup_type', 'cleanup_type', 'STRING', true, false, true, false, 13, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'cleanup_sql', 'cleanup_sql', 'STRING', false, false, true, false, 14, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'cleanup_procedure_name', 'cleanup_procedure_name', 'STRING', false, false, true, false, 15, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'layout_type', 'layout_type', 'STRING', true, false, true, false, 16, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'layout_count', 'layout_count', 'INTEGER', false, false, true, false, 17, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'file_name', 'file_name', 'STRING', false, false, true, false, 18, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'output_format', 'output_format', 'STRING', true, false, true, false, 19, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'use_signature', 'use_signature', 'BOOLEAN', true, false, true, false, 20, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'preview_enabled', 'preview_enabled', 'BOOLEAN', true, false, true, false, 21, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'active_flag', 'active_flag', 'BOOLEAN', true, false, true, false, 22, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'tenant_id', 'tenant_id', 'STRING', true, false, true, false, 23, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'created_at', 'created_at', 'DATETIME', true, false, true, false, 24, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'updated_at', 'updated_at', 'DATETIME', true, false, true, false, 25, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_MASTER';


-- =========================================
-- import_export_target : report_param
-- =========================================
INSERT INTO import_export_target (
    target_code,
    target_name,
    table_name,
    description,
    backup_enabled,
    import_enabled,
    active_flag,
    tenant_id,
    created_at,
    updated_at
) VALUES (
    'BACKUP_REPORT_PARAM',
    '帳票パラメータ',
    'report_param',
    '帳票パラメータのバックアップ出力',
    true,
    false,
    true,
    'default',
    NOW(),
    NOW()
);

-- =========================================
-- import_export_column : report_param
-- =========================================
INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'id', 'id', 'LONG', true, true, true, false, 1, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'report_master_id', 'report_master_id', 'LONG', true, true, true, false, 2, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'param_name', 'param_name', 'STRING', true, false, true, false, 3, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'param_label', 'param_label', 'STRING', true, false, true, false, 4, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'param_type', 'param_type', 'STRING', true, false, true, false, 5, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'control_type', 'control_type', 'STRING', true, false, true, false, 6, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'required_flag', 'required_flag', 'BOOLEAN', true, false, true, false, 7, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'visible_flag', 'visible_flag', 'BOOLEAN', true, false, true, false, 8, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'multiple_flag', 'multiple_flag', 'BOOLEAN', true, false, true, false, 9, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'filter_flag', 'filter_flag', 'BOOLEAN', true, false, true, false, 10, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'default_value', 'default_value', 'STRING', false, false, true, false, 11, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'placeholder', 'placeholder', 'STRING', false, false, true, false, 12, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'input_column_name', 'input_column_name', 'STRING', false, false, true, false, 13, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'display_order', 'display_order', 'INTEGER', true, false, true, false, 14, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'active_flag', 'active_flag', 'BOOLEAN', true, false, true, false, 15, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'tenant_id', 'tenant_id', 'STRING', true, false, true, false, 16, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'created_at', 'created_at', 'DATETIME', true, false, true, false, 17, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'updated_at', 'updated_at', 'DATETIME', true, false, true, false, 18, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_PARAM';


-- =========================================
-- import_export_target : report_signature
-- =========================================
INSERT INTO import_export_target (
    target_code,
    target_name,
    table_name,
    description,
    backup_enabled,
    import_enabled,
    active_flag,
    tenant_id,
    created_at,
    updated_at
) VALUES (
    'BACKUP_REPORT_SIGNATURE',
    '帳票署名',
    'report_signature',
    '帳票署名のバックアップ出力',
    true,
    false,
    true,
    'default',
    NOW(),
    NOW()
);

-- =========================================
-- import_export_column : report_signature
-- =========================================
INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'id', 'id', 'LONG', true, true, true, false, 1, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'report_master_id', 'report_master_id', 'LONG', true, true, true, false, 2, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'signature_type', 'signature_type', 'STRING', true, false, true, false, 3, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'signature_name', 'signature_name', 'STRING', true, false, true, false, 4, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'file_name', 'file_name', 'STRING', false, false, true, false, 5, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'content_type', 'content_type', 'STRING', false, false, true, false, 6, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'signature_image_data', 'signature_image_data', 'STRING', false, false, true, false, 7, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'width', 'width', 'INTEGER', false, false, true, false, 8, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'height', 'height', 'INTEGER', false, false, true, false, 9, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'display_order', 'display_order', 'INTEGER', true, false, true, false, 10, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'default_flag', 'default_flag', 'BOOLEAN', true, false, true, false, 11, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'active_flag', 'active_flag', 'BOOLEAN', true, false, true, false, 12, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'tenant_id', 'tenant_id', 'STRING', true, false, true, false, 13, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'created_at', 'created_at', 'DATETIME', true, false, true, false, 14, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';

INSERT INTO import_export_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, export_flag, import_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'updated_at', 'updated_at', 'DATETIME', true, false, true, false, 15, 'default', NOW(), NOW()
FROM import_export_target
WHERE target_code = 'BACKUP_REPORT_SIGNATURE';