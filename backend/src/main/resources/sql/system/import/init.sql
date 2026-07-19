DELETE c
FROM import_column c
INNER JOIN import_target t ON t.id = c.target_id
WHERE t.target_code = 'IMPORT_INCOME_TAX_TABLE';

DELETE FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

INSERT INTO import_target (
    target_code,
    target_name,
    table_name,
    description,
    source_type,
    fixed_file_path,
    script_type,
    script_path,
    script_args,
    import_mode,
    header_row_number,
    data_start_row_number,
    charset,
    delimiter,
    active_flag,
    tenant_id,
    created_at,
    updated_at
) VALUES (
    'IMPORT_INCOME_TAX_TABLE',
    '源泉徴収税額表取込',
    'income_tax_table',
    'Pythonで国税庁ExcelからCSVを生成し、源泉徴収税額表を取り込む定義',
    'SCRIPT',
    '/tmp/project-admin/import/income_tax_table_2026.csv',
    'PYTHON',
    '/app/storage/imports/scripts/convert_income_tax_table.py',
    '--year 2026 --input /tmp/project-admin/source/nta_income_tax_monthly_2026.xlsx --output /tmp/project-admin/import/income_tax_table_2026.csv',
    'INSERT_ONLY',
    1,
    2,
    'UTF-8',
    ',',
    true,
    'default',
    NOW(),
    NOW()
);

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'year', 'year', 'INTEGER',
       true, true, false, true,
       NULL, NULL, true,
       1, 'default', NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'min_salary', 'minSalary', 'INTEGER',
       true, true, false, true,
       NULL, NULL, true,
       2, 'default', NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'max_salary', 'maxSalary', 'INTEGER',
       true, true, false, true,
       NULL, NULL, true,
       3, 'default', NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'dependents', 'dependents', 'INTEGER',
       true, true, false, true,
       NULL, NULL, true,
       4, 'default', NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'tax_amount', 'taxAmount', 'INTEGER',
       true, false, false, true,
       NULL, NULL, true,
       5, 'default', NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

UPDATE import_target
SET
    source_type = 'SCRIPT',
    script_type = 'PYTHON',
    script_path = '/app/storage/imports/scripts/convert_income_tax_table.py',
    script_args = '--year 2026 --url https://www.nta.go.jp/publication/pamph/gensen/zeigakuhyo2026/data/01-07.xls --source /tmp/project-admin/source/nta_income_tax_monthly_2026.xls --output /tmp/project-admin/import/income_tax_table_2026.csv',
    fixed_file_path = '/tmp/project-admin/import/income_tax_table_2026.csv'
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';