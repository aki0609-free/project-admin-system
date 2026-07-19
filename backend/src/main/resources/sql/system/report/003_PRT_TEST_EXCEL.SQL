-- =========================
-- テーブル作成
-- =========================
CREATE TABLE IF NOT EXISTS test_excel_input (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    employee_code VARCHAR(50),
    employee_name VARCHAR(100),
    department_name VARCHAR(100),
    salary DECIMAL(15,2),
    tenant_id VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_test_excel_input_execution_id (execution_id)
);

CREATE TABLE IF NOT EXISTS test_excel_output (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    employee_code VARCHAR(50),
    employee_name VARCHAR(100),
    department_name VARCHAR(100),
    salary DECIMAL(15,2),
    tax_amount DECIMAL(15,2),
    net_amount DECIMAL(15,2),
    tenant_id VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_test_excel_output_execution_id (execution_id)
);

-- =========================
-- 既存削除
-- =========================
DELETE rp
FROM report_param rp
INNER JOIN report_master rm
    ON rm.id = rp.report_master_id
WHERE rm.report_code = 'RPT_TEST_EXCEL';

DELETE FROM report_master
WHERE report_code = 'RPT_TEST_EXCEL';

-- =========================
-- report_master
-- =========================
INSERT INTO report_master (
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
    tenant_id,
    created_at,
    updated_at
) VALUES (
    'RPT_TEST_EXCEL',
    'テストExcel出力',
    NULL,
    'test_excel',
    NULL,
    NULL,
    'SQL',
    '
INSERT INTO test_excel_output (
    execution_id,
    employee_code,
    employee_name,
    department_name,
    salary,
    tax_amount,
    net_amount,
    tenant_id,
    created_at,
    updated_at
)
SELECT
    execution_id,
    employee_code,
    employee_name,
    department_name,
    salary,
    ROUND(salary * 0.1, 2),
    salary - ROUND(salary * 0.1, 2),
    tenant_id,
    NOW(),
    NOW()
FROM test_excel_input
WHERE execution_id = :executionId
',
    NULL,
    '/tmp/reports',
    '
SELECT
    employee_code,
    employee_name,
    department_name,
    salary,
    tax_amount,
    net_amount
FROM test_excel_output
WHERE execution_id = :executionId
ORDER BY id
',
    'SQL',
    '
DELETE FROM test_excel_output
WHERE execution_id = :executionId
',
    NULL,
    'SINGLE',
    1,
    'test_excel.xlsx',
    'EXCEL',
    false,
    false,
    true,
    'default',
    NOW(),
    NOW()
);

-- =========================
-- report_param : employeeCode
-- =========================
INSERT INTO report_param (
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
    tenant_id,
    created_at,
    updated_at
)
SELECT
    id,
    'employeeCode',
    '社員コード',
    'STRING',
    'TEXT',
    true,
    true,
    false,
    true,
    'E001',
    '社員コードを入力',
    'employee_code',
    1,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_EXCEL';

-- =========================
-- report_param : employeeName
-- =========================
INSERT INTO report_param (
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
    tenant_id,
    created_at,
    updated_at
)
SELECT
    id,
    'employeeName',
    '社員名',
    'STRING',
    'TEXT',
    true,
    true,
    false,
    true,
    '山田太郎',
    '社員名を入力',
    'employee_name',
    2,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_EXCEL';

-- =========================
-- report_param : departmentName
-- =========================
INSERT INTO report_param (
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
    tenant_id,
    created_at,
    updated_at
)
SELECT
    id,
    'departmentName',
    '部署名',
    'STRING',
    'TEXT',
    true,
    true,
    false,
    true,
    '総務部',
    '部署名を入力',
    'department_name',
    3,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_EXCEL';

-- =========================
-- report_param : salary
-- =========================
INSERT INTO report_param (
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
    tenant_id,
    created_at,
    updated_at
)
SELECT
    id,
    'salary',
    '支給額',
    'DECIMAL',
    'NUMBER',
    true,
    true,
    false,
    true,
    '300000',
    '支給額を入力',
    'salary',
    4,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_EXCEL';