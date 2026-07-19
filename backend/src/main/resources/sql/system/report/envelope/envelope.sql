-- =========================================================
-- 封筒印刷 帳票 初期化
-- 冪等対応版
-- =========================================================

-- =========================================================
-- 既存データ削除
-- =========================================================
DELETE FROM report_history
WHERE report_master_id IN (
    SELECT id
    FROM report_master
    WHERE report_code IN ('ENVELOPE_NAGA3', 'ENVELOPE_KAKU2')
);

DELETE FROM report_param
WHERE report_master_id IN (
    SELECT id
    FROM report_master
    WHERE report_code IN (
        'ENVELOPE_NAGA3',
        'ENVELOPE_KAKU2'
    )
);

DELETE FROM report_master
WHERE report_code IN (
    'ENVELOPE_NAGA3',
    'ENVELOPE_KAKU2'
);

DROP TABLE IF EXISTS envelope_print_output;
DROP TABLE IF EXISTS envelope_print_input;



-- =========================================================
-- 封筒印刷 入力テーブル
-- =========================================================

CREATE TABLE envelope_print_input (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    tenant_id VARCHAR(50) NOT NULL,
    execution_id VARCHAR(100) NOT NULL,

    customer_id BIGINT NOT NULL,

    stamp VARCHAR(100),
    honorific VARCHAR(50),

    font_family VARCHAR(100),
    font_size INT,

    envelope_type VARCHAR(30),

    created_at DATETIME,
    updated_at DATETIME,
    deleted_at DATETIME
);

CREATE INDEX idx_envelope_print_input_execution_id
    ON envelope_print_input(execution_id);



-- =========================================================
-- 封筒印刷 出力テーブル
-- =========================================================

CREATE TABLE envelope_print_output (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    tenant_id VARCHAR(50) NOT NULL,
    execution_id VARCHAR(100) NOT NULL,

    customer_id BIGINT,

    customer_name VARCHAR(255),
    postal_code VARCHAR(20),
    address VARCHAR(500),

    stamp VARCHAR(100),
    honorific VARCHAR(50),

    font_family VARCHAR(100),
    font_size INT,

    envelope_type VARCHAR(30),

    created_at DATETIME,
    updated_at DATETIME,
    deleted_at DATETIME
);

CREATE INDEX idx_envelope_print_output_execution_id
    ON envelope_print_output(execution_id);



-- =========================================================
-- 帳票マスター : 長3封筒
-- =========================================================

INSERT INTO report_master (
    tenant_id,
    report_code,
    report_name,

    template_file_name,

    work_table,

    pre_process_type,
    pre_process_sql,

    query_sql,

    cleanup_type,
    cleanup_sql,

    output_path,

    layout_type,
    layout_count,

    file_name,
    output_format,

    use_signature,
    preview_enabled,
    active_flag,

    created_at,
    updated_at
)
VALUES (
    'default',

    'ENVELOPE_NAGA3',
    '封筒印刷 長形3号',

    'envelope_naga3.jrxml',

    'envelope_print_input',

    'SQL',

'
INSERT INTO envelope_print_output (
    tenant_id,
    execution_id,
    customer_id,
    customer_name,
    postal_code,
    address,
    stamp,
    honorific,
    font_family,
    font_size,
    envelope_type,
    created_at,
    updated_at
)
SELECT
    i.tenant_id,
    i.execution_id,
    c.id,
    c.name,
    c.post_no,
    c.address,
    COALESCE(i.stamp, c.envelope_stamp),
    i.honorific,
    i.font_family,
    i.font_size,
    i.envelope_type,
    NOW(),
    NOW()
FROM envelope_print_input i
INNER JOIN customers c
    ON c.id = i.customer_id
WHERE i.execution_id = :executionId
',

'
SELECT
    customer_name AS customerName,
    postal_code AS postalCode,
    address AS address,
    stamp AS stamp,
    honorific AS honorific,
    font_family AS fontFamily,
    font_size AS fontSize,
    envelope_type AS envelopeType
FROM envelope_print_output
WHERE execution_id = :executionId
ORDER BY id
',

    'SQL',

'
DELETE FROM envelope_print_output
WHERE execution_id = :executionId;

DELETE FROM envelope_print_input
WHERE execution_id = :executionId;
',

    '/reports/envelope',

    'SINGLE',
    1,

    'envelope_naga3',
    'PDF',

    false,
    true,
    true,

    NOW(),
    NOW()
);



-- =========================================================
-- 帳票マスター : 角2封筒
-- =========================================================

INSERT INTO report_master (
    tenant_id,
    report_code,
    report_name,

    template_file_name,

    work_table,

    pre_process_type,
    pre_process_sql,

    query_sql,

    cleanup_type,
    cleanup_sql,

    output_path,

    layout_type,
    layout_count,

    file_name,
    output_format,

    use_signature,
    preview_enabled,
    active_flag,

    created_at,
    updated_at
)
VALUES (
    'default',

    'ENVELOPE_KAKU2',
    '封筒印刷 角形2号',

    'envelope_kaku2.jrxml',

    'envelope_print_input',

    'SQL',

'
INSERT INTO envelope_print_output (
    tenant_id,
    execution_id,
    customer_id,
    customer_name,
    postal_code,
    address,
    stamp,
    honorific,
    font_family,
    font_size,
    envelope_type,
    created_at,
    updated_at
)
SELECT
    i.tenant_id,
    i.execution_id,
    c.id,
    c.name,
    c.post_no,
    c.address,
    COALESCE(i.stamp, c.envelope_stamp),
    i.honorific,
    i.font_family,
    i.font_size,
    i.envelope_type,
    NOW(),
    NOW()
FROM envelope_print_input i
INNER JOIN customers c
    ON c.id = i.customer_id
WHERE i.execution_id = :executionId
',

'
SELECT
    customer_name AS customerName,
    postal_code AS postalCode,
    address AS address,
    stamp AS stamp,
    honorific AS honorific,
    font_family AS fontFamily,
    font_size AS fontSize,
    envelope_type AS envelopeType
FROM envelope_print_output
WHERE execution_id = :executionId
ORDER BY id
',

    'SQL',

'
DELETE FROM envelope_print_output
WHERE execution_id = :executionId;

DELETE FROM envelope_print_input
WHERE execution_id = :executionId;
',

    '/reports/envelope',

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



-- =========================================================
-- 帳票パラメータ : customerIds
-- =========================================================

INSERT INTO report_param (
    tenant_id,
    report_master_id,

    param_name,
    param_label,

    param_type,
    input_type,

    required_flag,
    multiple_flag,
    active_flag,

    display_order,

    created_at,
    updated_at
)
SELECT
    'default',
    rm.id,

    'customerIds',
    '顧客',

    'LONG',
    'SELECT',

    true,
    true,
    true,

    1,

    NOW(),
    NOW()
FROM report_master rm
WHERE rm.report_code IN (
    'ENVELOPE_NAGA3',
    'ENVELOPE_KAKU2'
);



-- =========================================================
-- 帳票パラメータ : stamp
-- =========================================================

INSERT INTO report_param (
    tenant_id,
    report_master_id,

    param_name,
    param_label,

    param_type,
    input_type,

    required_flag,
    multiple_flag,
    active_flag,

    display_order,

    created_at,
    updated_at
)
SELECT
    'default',
    rm.id,

    'stamp',
    '封筒スタンプ',

    'STRING',
    'TEXT',

    false,
    false,
    true,

    2,

    NOW(),
    NOW()
FROM report_master rm
WHERE rm.report_code IN (
    'ENVELOPE_NAGA3',
    'ENVELOPE_KAKU2'
);



-- =========================================================
-- 帳票パラメータ : honorific
-- =========================================================

INSERT INTO report_param (
    tenant_id,
    report_master_id,

    param_name,
    param_label,

    param_type,
    input_type,

    required_flag,
    multiple_flag,
    active_flag,

    display_order,

    created_at,
    updated_at
)
SELECT
    'default',
    rm.id,

    'honorific',
    '敬称',

    'STRING',
    'SELECT',

    true,
    false,
    true,

    3,

    NOW(),
    NOW()
FROM report_master rm
WHERE rm.report_code IN (
    'ENVELOPE_NAGA3',
    'ENVELOPE_KAKU2'
);



-- =========================================================
-- 帳票パラメータ : fontFamily
-- =========================================================

INSERT INTO report_param (
    tenant_id,
    report_master_id,

    param_name,
    param_label,

    param_type,
    input_type,

    required_flag,
    multiple_flag,
    active_flag,

    display_order,

    created_at,
    updated_at
)
SELECT
    'default',
    rm.id,

    'fontFamily',
    'フォント',

    'STRING',
    'SELECT',

    true,
    false,
    true,

    4,

    NOW(),
    NOW()
FROM report_master rm
WHERE rm.report_code IN (
    'ENVELOPE_NAGA3',
    'ENVELOPE_KAKU2'
);



-- =========================================================
-- 帳票パラメータ : fontSize
-- =========================================================

INSERT INTO report_param (
    tenant_id,
    report_master_id,

    param_name,
    param_label,

    param_type,
    input_type,

    required_flag,
    multiple_flag,
    active_flag,

    display_order,

    created_at,
    updated_at
)
SELECT
    'default',
    rm.id,

    'fontSize',
    'フォントサイズ',

    'INTEGER',
    'NUMBER',

    true,
    false,
    true,

    5,

    NOW(),
    NOW()
FROM report_master rm
WHERE rm.report_code IN (
    'ENVELOPE_NAGA3',
    'ENVELOPE_KAKU2'
);



-- =========================================================
-- 帳票パラメータ : envelopeType
-- =========================================================

INSERT INTO report_param (
    tenant_id,
    report_master_id,

    param_name,
    param_label,

    param_type,
    input_type,

    required_flag,
    multiple_flag,
    active_flag,

    display_order,

    created_at,
    updated_at
)
SELECT
    'default',
    rm.id,

    'envelopeType',
    '封筒タイプ',

    'STRING',
    'SELECT',

    true,
    false,
    true,

    6,

    NOW(),
    NOW()
FROM report_master rm
WHERE rm.report_code IN (
    'ENVELOPE_NAGA3',
    'ENVELOPE_KAKU2'
);