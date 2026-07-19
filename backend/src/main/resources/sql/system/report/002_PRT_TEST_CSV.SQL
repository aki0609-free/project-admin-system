-- =========================
-- テーブル作成
-- =========================
CREATE TABLE IF NOT EXISTS test_csv_input (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    category VARCHAR(100),
    item_name VARCHAR(200),
    amount DECIMAL(15,2),
    tenant_id VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_test_csv_input_execution_id (execution_id)
);

CREATE TABLE IF NOT EXISTS test_csv_output (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    category VARCHAR(100),
    item_name VARCHAR(200),
    amount DECIMAL(15,2),
    output_no INT,
    tenant_id VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_test_csv_output_execution_id (execution_id)
);

-- =========================
-- 既存データ削除
-- =========================
DELETE rp
FROM report_param rp
INNER JOIN report_master rm
    ON rm.id = rp.report_master_id
WHERE rm.report_code = 'RPT_TEST_CSV';

DELETE FROM report_master
WHERE report_code = 'RPT_TEST_CSV';

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
    'RPT_TEST_CSV',
    'テストCSV出力',
    NULL,
    'test_csv',
    NULL,
    NULL,
    'SQL',
    '
INSERT INTO test_csv_output (
    execution_id,
    category,
    item_name,
    amount,
    output_no,
    tenant_id,
    created_at,
    updated_at
)
SELECT
    execution_id,
    category,
    item_name,
    amount,
    1,
    tenant_id,
    NOW(),
    NOW()
FROM test_csv_input
WHERE execution_id = :executionId

UNION ALL

SELECT
    execution_id,
    category,
    CONCAT(item_name, ''（控え）''),
    amount,
    2,
    tenant_id,
    NOW(),
    NOW()
FROM test_csv_input
WHERE execution_id = :executionId
',
    NULL,
    '/tmp/reports',
    '
SELECT
    output_no,
    category,
    item_name,
    amount
FROM test_csv_output
WHERE execution_id = :executionId
ORDER BY output_no, id
',
    'SQL',
    '
DELETE FROM test_csv_output
WHERE execution_id = :executionId
',
    NULL,
    'SINGLE',
    1,
    'test_csv.csv',
    'CSV',
    false,
    false,
    true,
    'default',
    NOW(),
    NOW()
);

-- =========================
-- report_param
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
    'category',
    'カテゴリ',
    'STRING',
    'TEXT',
    true,
    true,
    false,
    true,
    '備品',
    'カテゴリを入力',
    'category',
    1,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_CSV';

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
    'itemName',
    '品目名',
    'STRING',
    'TEXT',
    true,
    true,
    false,
    true,
    'ノートPC',
    '品目名を入力',
    'item_name',
    2,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_CSV';

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
    'amount',
    '金額',
    'DECIMAL',
    'NUMBER',
    true,
    true,
    false,
    true,
    '128000',
    '金額を入力',
    'amount',
    3,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_CSV';