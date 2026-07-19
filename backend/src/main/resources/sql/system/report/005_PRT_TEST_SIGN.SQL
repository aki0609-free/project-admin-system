-- =========================
-- テーブル作成
-- =========================
CREATE TABLE IF NOT EXISTS test_sign_input (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    message VARCHAR(500),
    target_name VARCHAR(200),
    tenant_id VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_test_sign_input_execution_id (execution_id)
);

CREATE TABLE IF NOT EXISTS test_sign_output (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    message VARCHAR(500),
    target_name VARCHAR(200),
    printed_at DATETIME,
    tenant_id VARCHAR(50),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_test_sign_output_execution_id (execution_id)
);

-- =========================
-- 既存データ削除
-- =========================
DELETE rp
FROM report_param rp
INNER JOIN report_master rm
    ON rm.id = rp.report_master_id
WHERE rm.report_code = 'RPT_TEST_SIGN';

DELETE rs
FROM report_signature rs
INNER JOIN report_master rm
    ON rm.id = rs.report_master_id
WHERE rm.report_code = 'RPT_TEST_SIGN';

DELETE FROM report_master
WHERE report_code = 'RPT_TEST_SIGN';

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
    'RPT_TEST_SIGN',
    'テスト帳票（署名付き）',
    'test_sign.jrxml',
    'test_sign',
    NULL,
    NULL,
    'SQL',
    '
INSERT INTO test_sign_output (
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
FROM test_sign_input
WHERE execution_id = :executionId
',
    NULL,
    '/tmp/reports',
    '
SELECT
    message,
    target_name,
    printed_at
FROM test_sign_output
WHERE execution_id = :executionId
ORDER BY id
',
    'SQL',
    '
DELETE FROM test_sign_output
WHERE execution_id = :executionId
',
    NULL,
    'SINGLE',
    1,
    'test_sign.pdf',
    'PDF',
    true,
    true,
    true,
    'default',
    NOW(),
    NOW()
);

-- =========================
-- report_param : message
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
    'message',
    'メッセージ',
    'STRING',
    'TEXT',
    true,
    true,
    false,
    true,
    '署名付き帳票テストです',
    'メッセージを入力',
    'message',
    1,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_SIGN';

-- =========================
-- report_param : targetName
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
WHERE report_code = 'RPT_TEST_SIGN';

-- =========================
-- 署名レコード（画像なし）
-- =========================
INSERT INTO report_signature (
    report_master_id,
    signature_type,
    signature_name,
    file_name,
    content_type,
    signature_image_data,
    width,
    height,
    display_order,
    default_flag,
    active_flag,
    tenant_id,
    created_at,
    updated_at
)
SELECT
    rm.id,
    'STAMP',
    'テスト署名（未設定）',
    '',
    '',
    '',
    120,
    60,
    1,
    true,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master rm
WHERE rm.report_code = 'RPT_TEST_SIGN';