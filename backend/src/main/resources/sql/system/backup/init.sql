-- =========================================
-- backup_target / backup_column : 既存削除
-- =========================================
DELETE c
FROM backup_column c
INNER JOIN backup_target t
    ON t.id = c.target_id
WHERE t.target_code IN (
    'BACKUP_REPORT_MASTER',
    'BACKUP_REPORT_PARAM',
    'BACKUP_REPORT_SIGNATURE'
);

DELETE FROM backup_target
WHERE target_code IN (
    'BACKUP_REPORT_MASTER',
    'BACKUP_REPORT_PARAM',
    'BACKUP_REPORT_SIGNATURE'
);

-- =========================================
-- backup_target : report_master
-- =========================================
INSERT INTO backup_target (
    target_code,
    target_name,
    table_name,
    description,
    backup_enabled,
    active_flag,
    output_mode,
    output_dir,
    file_name_pattern,
    zip_required,
    tenant_id,
    created_at,
    updated_at
) VALUES (
    'BACKUP_REPORT_MASTER',
    '帳票マスタ',
    'report_master',
    '帳票マスタのバックアップ出力',
    true,
    true,
    'DOWNLOAD',
    null,
    '{targetCode}_{timestamp}.csv',
    false,
    'default',
    NOW(),
    NOW()
);

-- =========================================
-- backup_column : report_master
-- =========================================
INSERT INTO backup_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    export_flag,
    order_no,
    tenant_id,
    created_at,
    updated_at
)
SELECT
    id,
    'id',
    'id',
    'LONG',
    true,
    1,
    'default',
    NOW(),
    NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'report_code', 'report_code', 'STRING', true, 2, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'report_name', 'report_name', 'STRING', true, 3, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'template_file_name', 'template_file_name', 'STRING', true, 4, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'work_table', 'work_table', 'STRING', true, 5, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'input_table', 'input_table', 'STRING', true, 6, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'output_table', 'output_table', 'STRING', true, 7, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'pre_process_type', 'pre_process_type', 'STRING', true, 8, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'pre_process_sql', 'pre_process_sql', 'STRING', true, 9, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'procedure_name', 'procedure_name', 'STRING', true, 10, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'output_path', 'output_path', 'STRING', true, 11, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'query_sql', 'query_sql', 'STRING', true, 12, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'cleanup_type', 'cleanup_type', 'STRING', true, 13, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'cleanup_sql', 'cleanup_sql', 'STRING', true, 14, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'cleanup_procedure_name', 'cleanup_procedure_name', 'STRING', true, 15, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'layout_type', 'layout_type', 'STRING', true, 16, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'layout_count', 'layout_count', 'INTEGER', true, 17, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'file_name', 'file_name', 'STRING', true, 18, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'output_format', 'output_format', 'STRING', true, 19, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'use_signature', 'use_signature', 'BOOLEAN', true, 20, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'preview_enabled', 'preview_enabled', 'BOOLEAN', true, 21, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'active_flag', 'active_flag', 'BOOLEAN', true, 22, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'tenant_id', 'tenant_id', 'STRING', true, 23, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'created_at', 'created_at', 'DATETIME', true, 24, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'updated_at', 'updated_at', 'DATETIME', true, 25, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

-- =========================================
-- backup_target / backup_column : 既存削除
-- =========================================
DELETE c
FROM backup_column c
INNER JOIN backup_target t
    ON t.id = c.target_id
WHERE t.target_code IN (
    'BACKUP_REPORT_MASTER',
    'BACKUP_REPORT_PARAM',
    'BACKUP_REPORT_SIGNATURE'
);

DELETE FROM backup_target
WHERE target_code IN (
    'BACKUP_REPORT_MASTER',
    'BACKUP_REPORT_PARAM',
    'BACKUP_REPORT_SIGNATURE'
);

-- =========================================
-- backup_target : report_master
-- =========================================
INSERT INTO backup_target (
    target_code,
    target_name,
    table_name,
    description,
    backup_enabled,
    active_flag,
    output_mode,
    output_dir,
    file_name_pattern,
    zip_required,
    tenant_id,
    created_at,
    updated_at
) VALUES (
    'BACKUP_REPORT_MASTER',
    '帳票マスタ',
    'report_master',
    '帳票マスタのバックアップ出力',
    true,
    true,
    'DOWNLOAD',
    null,
    '{targetCode}_{timestamp}.csv',
    false,
    'default',
    NOW(),
    NOW()
);

-- =========================================
-- backup_column : report_master
-- =========================================
INSERT INTO backup_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    export_flag,
    order_no,
    tenant_id,
    created_at,
    updated_at
)
SELECT
    id,
    'id',
    'id',
    'LONG',
    true,
    1,
    'default',
    NOW(),
    NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'report_code', 'report_code', 'STRING', true, 2, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'report_name', 'report_name', 'STRING', true, 3, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'template_file_name', 'template_file_name', 'STRING', true, 4, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'work_table', 'work_table', 'STRING', true, 5, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'input_table', 'input_table', 'STRING', true, 6, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'output_table', 'output_table', 'STRING', true, 7, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'pre_process_type', 'pre_process_type', 'STRING', true, 8, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'pre_process_sql', 'pre_process_sql', 'STRING', true, 9, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'procedure_name', 'procedure_name', 'STRING', true, 10, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'output_path', 'output_path', 'STRING', true, 11, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'query_sql', 'query_sql', 'STRING', true, 12, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'cleanup_type', 'cleanup_type', 'STRING', true, 13, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'cleanup_sql', 'cleanup_sql', 'STRING', true, 14, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'cleanup_procedure_name', 'cleanup_procedure_name', 'STRING', true, 15, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'layout_type', 'layout_type', 'STRING', true, 16, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'layout_count', 'layout_count', 'INTEGER', true, 17, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'file_name', 'file_name', 'STRING', true, 18, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'output_format', 'output_format', 'STRING', true, 19, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'use_signature', 'use_signature', 'BOOLEAN', true, 20, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'preview_enabled', 'preview_enabled', 'BOOLEAN', true, 21, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'active_flag', 'active_flag', 'BOOLEAN', true, 22, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'tenant_id', 'tenant_id', 'STRING', true, 23, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'created_at', 'created_at', 'DATETIME', true, 24, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

INSERT INTO backup_column (
    target_id, column_name, csv_header_name, data_type,
    export_flag, order_no, tenant_id, created_at, updated_at
)
SELECT id, 'updated_at', 'updated_at', 'DATETIME', true, 25, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_REPORT_MASTER';

-- =========================================
-- backup_target : user master
-- =========================================
INSERT INTO backup_target (
    target_code,
    target_name,
    table_name,
    description,
    backup_enabled,
    active_flag,
    output_mode,
    output_dir,
    file_name_pattern,
    zip_required,
    tenant_id,
    created_at,
    updated_at
) VALUES (
    'BACKUP_USERS',
    'ユーザマスタ',
    'users',
    'ユーザマスタのバックアップ出力',
    true,
    true,
    'DOWNLOAD',
    null,
    '{targetCode}_{timestamp}.csv',
    false,
    'default',
    NOW(),
    NOW()
);

DELETE c
FROM backup_column c
INNER JOIN backup_target t
    ON t.id = c.target_id
WHERE t.target_code = 'BACKUP_USERS';

INSERT INTO backup_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    export_flag,
    order_no,
    tenant_id,
    created_at,
    updated_at
)
SELECT id, 'id', 'id', 'LONG', true, 1, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_USERS';

INSERT INTO backup_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    export_flag,
    order_no,
    tenant_id,
    created_at,
    updated_at
)
SELECT id, 'username', 'username', 'STRING', true, 2, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_USERS';

INSERT INTO backup_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    export_flag,
    order_no,
    tenant_id,
    created_at,
    updated_at
)
SELECT id, 'password', 'password', 'STRING', true, 3, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_USERS';

INSERT INTO backup_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    export_flag,
    order_no,
    tenant_id,
    created_at,
    updated_at
)
SELECT id, 'enabled', 'enabled', 'BOOLEAN', true, 4, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_USERS';

INSERT INTO backup_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    export_flag,
    order_no,
    tenant_id,
    created_at,
   updated_at
)
SELECT id, 'tenant_id', 'tenant_id', 'STRING', true, 5, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_USERS';

INSERT INTO backup_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    export_flag,
    order_no,
    tenant_id,
    created_at,
    updated_at
)
SELECT id, 'created_at', 'created_at', 'DATETIME', true, 6, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_USERS';

INSERT INTO backup_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    export_flag,
    order_no,
    tenant_id,
    created_at,
    updated_at
)
SELECT id, 'updated_at', 'updated_at', 'DATETIME', true, 7, 'default', NOW(), NOW()
FROM backup_target
WHERE target_code = 'BACKUP_USERS';