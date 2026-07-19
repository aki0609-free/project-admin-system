-- =========================
-- テーブル作成
-- =========================
CREATE TABLE IF NOT EXISTS report_proc_test_input (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(100) NOT NULL,
    message VARCHAR(255),
    target_name VARCHAR(255),
    tenant_id VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    INDEX idx_report_proc_test_input_execution_id (execution_id)
);

CREATE TABLE IF NOT EXISTS report_proc_test_output (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(100) NOT NULL,
    line_no INT NOT NULL,
    message VARCHAR(255),
    target_name VARCHAR(255),
    printed_at DATETIME,
    tenant_id VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    INDEX idx_report_proc_test_output_execution_id (execution_id)
);

-- =========================
-- 既存データ削除
-- =========================
DELETE rh
FROM report_history rh
INNER JOIN report_master rm
    ON rm.id = rh.report_master_id
WHERE rm.report_code = 'RPT_TEST_PROC';

DELETE rp
FROM report_param rp
INNER JOIN report_master rm
    ON rm.id = rp.report_master_id
WHERE rm.report_code = 'RPT_TEST_PROC';

DELETE rs
FROM report_signature rs
INNER JOIN report_master rm
    ON rm.id = rs.report_master_id
WHERE rm.report_code = 'RPT_TEST_PROC';

DELETE FROM report_master
WHERE report_code = 'RPT_TEST_PROC';

-- =========================
-- Procedure再作成
-- =========================
DROP PROCEDURE IF EXISTS sp_prepare_test_report;

DELIMITER //

CREATE PROCEDURE sp_prepare_test_report(
    IN p_execution_id VARCHAR(100)
)
BEGIN
    INSERT INTO report_proc_test_output (
        execution_id,
        line_no,
        message,
        target_name,
        printed_at,
        tenant_id,
        created_at,
        updated_at
    )
    SELECT
        i.execution_id,
        1,
        i.message,
        i.target_name,
        NOW(),
        i.tenant_id,
        NOW(),
        NOW()
    FROM report_proc_test_input i
    WHERE i.execution_id = p_execution_id;
END //

DELIMITER ;

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
    'RPT_TEST_PROC',
    'テスト帳票（Procedure）',
    'test_proc.jrxml',
    'report_proc_test',
    NULL,
    NULL,
    'PROCEDURE',
    NULL,
    'sp_prepare_test_report',
    '/tmp/reports',
    '
SELECT
    line_no,
    message,
    target_name,
    printed_at
FROM report_proc_test_output
WHERE execution_id = :executionId
ORDER BY line_no, id
',
    'SQL',
    '
DELETE FROM report_proc_test_output
WHERE execution_id = :executionId
',
    NULL,
    'SINGLE',
    1,
    'test_proc.pdf',
    'PDF',
    false,
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
    'Procedureテストです',
    'メッセージを入力',
    'message',
    1,
    true,
    'default',
    NOW(),
    NOW()
FROM report_master
WHERE report_code = 'RPT_TEST_PROC';

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
WHERE report_code = 'RPT_TEST_PROC';