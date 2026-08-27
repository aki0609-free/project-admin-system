-- ProjectAdminSystem V1 税・社会保険データ取込定義
-- MySQL 8.x / 再実行可能

INSERT INTO import_target (
    target_code, target_name, table_name, description,
    source_type, fixed_file_path, script_type, script_path, script_args,
    import_mode, header_row_number, data_start_row_number,
    charset, delimiter, active_flag,
    tenant_id, created_at, updated_at, deleted_at
) VALUES
(
    'IMPORT_INCOME_TAX_TABLE', '源泉徴収税額表取込', 'income_tax_table',
    '国税庁の源泉徴収税額表を公式Excelから正規化して取り込む',
    'SCRIPT', 'income_tax_table_2026.csv', 'PYTHON',
    'convert_income_tax_table_v2.py',
    '--year 2026 --url https://www.nta.go.jp/publication/pamph/gensen/zeigakuhyo2026/data/01-07.xls --input ${IMPORT_WORK_DIR}/source/nta_income_tax_monthly_2026.xls --output ${IMPORT_CSV_DIR}/income_tax_table_2026.csv',
    'UPSERT', 1, 2, 'UTF-8', ',', TRUE,
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
),
(
    'IMPORT_HEALTH_INSURANCE_RATE', '健康保険料率取込',
    'insurance_rate_master', '協会けんぽ静岡県の健康保険料率を取り込む',
    'SCRIPT', 'health_insurance_rate_2026.csv', 'PYTHON',
    'convert_health_insurance_rate.py',
    '--year 2026 --url https://www.kyoukaikenpo.or.jp/assets/R8_22shizuoka.pdf --input ${IMPORT_WORK_DIR}/source/kyoukaikenpo_shizuoka_2026.pdf --output ${IMPORT_CSV_DIR}/health_insurance_rate_2026.csv',
    'UPSERT', 1, 2, 'UTF-8', ',', TRUE,
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
),
(
    'IMPORT_CARE_INSURANCE_RATE', '介護保険料率取込',
    'insurance_rate_master', '協会けんぽの介護保険料率を取り込む',
    'SCRIPT', 'care_insurance_rate_2026.csv', 'PYTHON',
    'convert_care_insurance_rate.py',
    '--year 2026 --url https://www.kyoukaikenpo.or.jp/assets/R8_22shizuoka.pdf --input ${IMPORT_WORK_DIR}/source/kyoukaikenpo_shizuoka_2026.pdf --output ${IMPORT_CSV_DIR}/care_insurance_rate_2026.csv',
    'UPSERT', 1, 2, 'UTF-8', ',', TRUE,
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
),
(
    'IMPORT_PENSION_INSURANCE_RATE', '厚生年金保険料率取込',
    'insurance_rate_master', '日本年金機構の厚生年金保険料率を取り込む',
    'SCRIPT', 'pension_insurance_rate_2026.csv', 'PYTHON',
    'convert_pension_insurance_rate.py',
    '--year 2026 --url https://www.nenkin.go.jp/service/kounen/hokenryo/ryogaku/ryogakuhyo/20200825.files/R08ryougaku.pdf --input ${IMPORT_WORK_DIR}/source/nenkin_pension_2026.pdf --output ${IMPORT_CSV_DIR}/pension_insurance_rate_2026.csv',
    'UPSERT', 1, 2, 'UTF-8', ',', TRUE,
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
),
(
    'IMPORT_EMPLOYMENT_INSURANCE_RATE', '雇用保険料率取込',
    'insurance_rate_master',
    '正式な労働保険事業区分の確認後にcategoryとactive_flagを確定する',
    'SCRIPT', 'employment_insurance_rate_2026.csv', 'PYTHON',
    'convert_employment_insurance_rate.py',
    '--year 2026 --category GENERAL --url https://www.mhlw.go.jp/content/001692566.pdf --input ${IMPORT_WORK_DIR}/source/mhlw_employment_insurance_2026.pdf --output ${IMPORT_CSV_DIR}/employment_insurance_rate_2026.csv',
    'UPSERT', 1, 2, 'UTF-8', ',', FALSE,
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
),
(
    'IMPORT_CHILD_CARE_SUPPORT_FUND', '子ども・子育て支援金取込',
    'insurance_rate_master', '協会けんぽの子ども・子育て支援金率を取り込む',
    'SCRIPT', 'child_care_support_fund_2026.csv', 'PYTHON',
    'convert_child_care_support_fund.py',
    '--year 2026 --url https://www.kyoukaikenpo.or.jp/assets/R8_22shizuoka.pdf --input ${IMPORT_WORK_DIR}/source/kyoukaikenpo_shizuoka_2026.pdf --output ${IMPORT_CSV_DIR}/child_care_support_fund_2026.csv',
    'UPSERT', 1, 2, 'UTF-8', ',', TRUE,
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
),
(
    'IMPORT_RESIDENT_TAX', '住民税取込', 'resident_tax_monthly',
    '住民税通知CSVを月別化して取り込む。原本は自動取得しない',
    'UPLOAD', 'resident_tax_2026.csv', 'PYTHON',
    'convert_resident_tax.py',
    '--year 2026 --input ${IMPORT_INPUT_FILE} --output ${IMPORT_CSV_DIR}/resident_tax_2026.csv',
    'UPSERT', 1, 2, 'UTF-8', ',', TRUE,
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
)
ON DUPLICATE KEY UPDATE
    target_name = VALUES(target_name),
    table_name = VALUES(table_name),
    description = VALUES(description),
    source_type = VALUES(source_type),
    fixed_file_path = VALUES(fixed_file_path),
    script_type = VALUES(script_type),
    script_path = VALUES(script_path),
    script_args = VALUES(script_args),
    import_mode = VALUES(import_mode),
    header_row_number = VALUES(header_row_number),
    data_start_row_number = VALUES(data_start_row_number),
    charset = VALUES(charset),
    delimiter = VALUES(delimiter),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL;

CREATE TEMPORARY TABLE tmp_tax_import_column (
    target_code VARCHAR(100) NOT NULL,
    column_name VARCHAR(200) NOT NULL,
    csv_header_name VARCHAR(200) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    required_flag TINYINT(1) NOT NULL,
    key_flag TINYINT(1) NOT NULL,
    nullable_flag TINYINT(1) NOT NULL,
    updatable_flag TINYINT(1) NOT NULL,
    order_no INT NOT NULL,
    PRIMARY KEY (target_code, column_name)
);

INSERT INTO tmp_tax_import_column VALUES
('IMPORT_INCOME_TAX_TABLE', 'year', 'year', 'INTEGER', 1, 1, 0, 0, 1),
('IMPORT_INCOME_TAX_TABLE', 'min_salary', 'minSalary', 'INTEGER', 1, 1, 0, 0, 2),
('IMPORT_INCOME_TAX_TABLE', 'max_salary', 'maxSalary', 'INTEGER', 1, 1, 0, 0, 3),
('IMPORT_INCOME_TAX_TABLE', 'dependents', 'dependents', 'INTEGER', 1, 1, 0, 0, 4),
('IMPORT_INCOME_TAX_TABLE', 'tax_amount', 'taxAmount', 'INTEGER', 1, 0, 0, 1, 5),
('IMPORT_RESIDENT_TAX', 'employee_id', 'employeeId', 'LONG', 1, 1, 0, 0, 1),
('IMPORT_RESIDENT_TAX', 'fiscal_year', 'fiscalYear', 'INTEGER', 1, 1, 0, 0, 2),
('IMPORT_RESIDENT_TAX', 'month', 'month', 'INTEGER', 1, 1, 0, 0, 3),
('IMPORT_RESIDENT_TAX', 'tax_amount', 'taxAmount', 'INTEGER', 1, 0, 0, 1, 4);

INSERT INTO tmp_tax_import_column
SELECT target_code, 'insurance_type', 'insuranceType', 'STRING', 1, 1, 0, 0, 1
FROM import_target
WHERE target_code IN (
    'IMPORT_HEALTH_INSURANCE_RATE', 'IMPORT_CARE_INSURANCE_RATE',
    'IMPORT_PENSION_INSURANCE_RATE', 'IMPORT_EMPLOYMENT_INSURANCE_RATE',
    'IMPORT_CHILD_CARE_SUPPORT_FUND'
)
UNION ALL
SELECT target_code, 'year', 'year', 'INTEGER', 1, 1, 0, 0, 2
FROM import_target
WHERE target_code IN (
    'IMPORT_HEALTH_INSURANCE_RATE', 'IMPORT_CARE_INSURANCE_RATE',
    'IMPORT_PENSION_INSURANCE_RATE', 'IMPORT_EMPLOYMENT_INSURANCE_RATE',
    'IMPORT_CHILD_CARE_SUPPORT_FUND'
)
UNION ALL
SELECT target_code, 'employee_rate', 'employeeRate', 'DECIMAL', 1, 0, 0, 1, 3
FROM import_target
WHERE target_code IN (
    'IMPORT_HEALTH_INSURANCE_RATE', 'IMPORT_CARE_INSURANCE_RATE',
    'IMPORT_PENSION_INSURANCE_RATE', 'IMPORT_EMPLOYMENT_INSURANCE_RATE',
    'IMPORT_CHILD_CARE_SUPPORT_FUND'
)
UNION ALL
SELECT target_code, 'employer_rate', 'employerRate', 'DECIMAL', 1, 0, 0, 1, 4
FROM import_target
WHERE target_code IN (
    'IMPORT_HEALTH_INSURANCE_RATE', 'IMPORT_CARE_INSURANCE_RATE',
    'IMPORT_PENSION_INSURANCE_RATE', 'IMPORT_EMPLOYMENT_INSURANCE_RATE',
    'IMPORT_CHILD_CARE_SUPPORT_FUND'
);

UPDATE import_column column_def
JOIN import_target target ON target.id = column_def.target_id
JOIN tmp_tax_import_column expected
  ON expected.target_code = target.target_code
 AND expected.column_name = column_def.column_name
SET
    column_def.csv_header_name = expected.csv_header_name,
    column_def.data_type = expected.data_type,
    column_def.required_flag = expected.required_flag,
    column_def.key_flag = expected.key_flag,
    column_def.nullable_flag = expected.nullable_flag,
    column_def.trim_flag = TRUE,
    column_def.updatable_flag = expected.updatable_flag,
    column_def.order_no = expected.order_no,
    column_def.updated_at = CURRENT_TIMESTAMP(6),
    column_def.deleted_at = NULL;

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag, order_no,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    target.id, expected.column_name, expected.csv_header_name,
    expected.data_type, expected.required_flag, expected.key_flag,
    expected.nullable_flag, TRUE,
    NULL, NULL, expected.updatable_flag, expected.order_no,
    target.tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tmp_tax_import_column expected
JOIN import_target target ON target.target_code = expected.target_code
WHERE NOT EXISTS (
    SELECT 1
    FROM import_column existing
    WHERE existing.target_id = target.id
      AND existing.column_name = expected.column_name
      AND existing.deleted_at IS NULL
);

INSERT INTO import_target_catalog (
    table_name, display_name, description,
    tenant_scoped_flag, allow_delete_insert_flag, active_flag,
    tenant_id, created_at, updated_at, deleted_at
) VALUES
('income_tax_table', '源泉徴収税額表', '公式所得税表', FALSE, FALSE, TRUE,
 'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL),
('insurance_rate_master', '社会保険料率', '公式社会保険料率', FALSE, FALSE, TRUE,
 'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL),
('resident_tax_monthly', '住民税月額', '従業員別住民税月額', TRUE, FALSE, TRUE,
 'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    tenant_scoped_flag = VALUES(tenant_scoped_flag),
    allow_delete_insert_flag = VALUES(allow_delete_insert_flag),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL;

INSERT INTO import_target_catalog_column (
    catalog_id, column_name, display_name, data_type,
    order_no, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    catalog.id,
    expected.column_name,
    MIN(expected.csv_header_name),
    MIN(expected.data_type),
    MIN(expected.order_no),
    TRUE,
    catalog.tenant_id,
    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tmp_tax_import_column expected
JOIN import_target target ON target.target_code = expected.target_code
JOIN import_target_catalog catalog
  ON catalog.tenant_id = target.tenant_id
 AND catalog.table_name = target.table_name
GROUP BY catalog.id, catalog.tenant_id, expected.column_name
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    data_type = VALUES(data_type),
    order_no = VALUES(order_no),
    active_flag = TRUE,
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL;

DROP TEMPORARY TABLE tmp_tax_import_column;
