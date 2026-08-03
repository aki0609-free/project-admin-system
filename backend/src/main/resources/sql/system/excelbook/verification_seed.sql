-- ProjectAdminSystem V1
-- ローカル／DEV動作確認用の台帳データソース
-- 本番マスタデータ確定後は業務用View・カタログへ置き換える。

CREATE OR REPLACE VIEW vw_excel_book_employee_verification AS
SELECT
    tenant_id,
    employee_code,
    employee_name,
    employment_status,
    employment_type,
    hire_date
FROM employee
WHERE deleted_at IS NULL;

INSERT INTO excel_book_data_source_catalog (
    source_code,
    display_name,
    physical_name,
    where_clause_template,
    tenant_scoped_flag,
    max_rows,
    description,
    active_flag,
    created_at,
    updated_at,
    tenant_id
) VALUES (
    'EMPLOYEE_VERIFICATION',
    '従業員一覧（台帳動作確認用）',
    'vw_excel_book_employee_verification',
    'tenant_id = :tenantId',
    1,
    1000,
    'Syncfusion Spreadsheet台帳基盤の動作確認専用',
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    'default'
)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    physical_name = VALUES(physical_name),
    where_clause_template = VALUES(where_clause_template),
    max_rows = VALUES(max_rows),
    description = VALUES(description),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO excel_book_data_source_catalog_column (
    catalog_id,
    column_name,
    display_name,
    data_type,
    order_no,
    active_flag,
    created_at,
    updated_at,
    tenant_id
)
SELECT
    catalog.id,
    seed.column_name,
    seed.display_name,
    seed.data_type,
    seed.order_no,
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    'default'
FROM excel_book_data_source_catalog catalog
JOIN (
    SELECT 'employee_code' column_name,
           '従業員コード' display_name,
           'STRING' data_type,
           1 order_no
    UNION ALL
    SELECT 'employee_name', '従業員名', 'STRING', 2
    UNION ALL
    SELECT 'employment_status', '在籍状況', 'STRING', 3
    UNION ALL
    SELECT 'employment_type', '雇用区分', 'STRING', 4
    UNION ALL
    SELECT 'hire_date', '入社日', 'DATE', 5
) seed
WHERE catalog.tenant_id = 'default'
  AND catalog.source_code = 'EMPLOYEE_VERIFICATION'
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    data_type = VALUES(data_type),
    order_no = VALUES(order_no),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6);
