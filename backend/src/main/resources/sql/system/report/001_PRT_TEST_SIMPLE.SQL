-- =========================================
-- 1. test_simple_input / test_simple_output
-- =========================================

CREATE TABLE IF NOT EXISTS test_simple_input (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    message VARCHAR(500),
    target_name VARCHAR(200),
    tenant_id VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_test_simple_input_execution_id (execution_id)
);

CREATE TABLE IF NOT EXISTS test_simple_output (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    message VARCHAR(500),
    target_name VARCHAR(200),
    printed_at DATETIME,
    tenant_id VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_test_simple_output_execution_id (execution_id)
);

-- =========================================
-- 2. 既存データ削除（入れ直し用）
-- =========================================

DELETE rp
FROM report_param rp
INNER JOIN report_master rm
    ON rm.id = rp.report_master_id
WHERE rm.report_code = 'RPT_TEST_SIMPLE';

DELETE FROM report_master
WHERE report_code = 'RPT_TEST_SIMPLE';

-- =========================================
-- 3. report_master
-- =========================================

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
    'RPT_TEST_SIMPLE',
    'テスト帳票（シンプル）',
    'test_simple.jrxml',
    'test_simple',
    NULL,
    NULL,
    'SQL',
    '
INSERT INTO test_simple_output (
    execution_id,
    message,
    target_name,
    printed_at,
    tenant_id,
    created_at,
    updated_at
)
SELECT
    execution_id,
    message,
    target_name,
    NOW(),
    tenant_id,
    NOW(),
    NOW()
FROM test_simple_input
WHERE execution_id = :executionId
',
    NULL,
    '/tmp/reports',
    '
SELECT
    message,
    target_name,
    printed_at
FROM test_simple_output
WHERE execution_id = :executionId
ORDER BY id
',
    'SQL',
    '
DELETE FROM test_simple_output
WHERE execution_id = :executionId
',
    NULL,
    'SINGLE',
    1,
    'test_simple.pdf',
    'PDF',
    false,
    true,
    true,
    'default',
    NOW(),
    NOW()
);

-- =========================================
-- 4. report_param: message
-- =========================================

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
    'message',
    'メッセージ',
    'STRING',
    'TEXT',
    true,
    true,
    false,
    true,
    'これはテスト帳票です',
    'メッセージを入力',
    'message',
    1,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_SIMPLE';

-- =========================================
-- 5. report_param: targetName
-- =========================================

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
    'targetName',
    '対象名',
    'STRING',
    'TEXT',
    true,
    true,
    false,
    true,
    'AKIさん',
    '対象名を入力',
    'target_name',
    2,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_SIMPLE';