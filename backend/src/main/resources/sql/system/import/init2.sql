-- =====================================================
-- Import Target Master: Tax / Social Insurance
-- =====================================================

DELETE c
FROM import_column c
INNER JOIN import_target t ON t.id = c.target_id
WHERE t.target_code IN (
    'IMPORT_INCOME_TAX_TABLE',
    'IMPORT_HEALTH_INSURANCE_RATE',
    'IMPORT_PENSION_INSURANCE_RATE',
    'IMPORT_EMPLOYMENT_INSURANCE_RATE',
    'IMPORT_STANDARD_MONTHLY_REMUNERATION',
    'IMPORT_CHILD_CARE_SUPPORT_FUND',
    'IMPORT_RESIDENT_TAX'
);

DELETE FROM import_target
WHERE target_code IN (
    'IMPORT_INCOME_TAX_TABLE',
    'IMPORT_HEALTH_INSURANCE_RATE',
    'IMPORT_PENSION_INSURANCE_RATE',
    'IMPORT_EMPLOYMENT_INSURANCE_RATE',
    'IMPORT_STANDARD_MONTHLY_REMUNERATION',
    'IMPORT_CHILD_CARE_SUPPORT_FUND',
    'IMPORT_RESIDENT_TAX'
);

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
)
VALUES
(
    'IMPORT_INCOME_TAX_TABLE',
    '源泉徴収税額表取込',
    'income_tax_table',
    '国税庁の源泉徴収税額表をPythonでCSV化して取り込む定義',
    'SCRIPT',
    '/tmp/project-admin/import/income_tax_table_2026.csv',
    'PYTHON',
    '/app/storage/imports/scripts/convert_income_tax_table.py',
    '--year 2026 --input /tmp/project-admin/source/nta_income_tax_monthly_2026.xls --output /tmp/project-admin/import/income_tax_table_2026.csv',
    'INSERT_ONLY',
    1,
    2,
    'UTF-8',
    ',',
    TRUE,
    'default',
    NOW(),
    NOW()
),
(
    'IMPORT_HEALTH_INSURANCE_RATE',
    '健康保険料率取込',
    'health_insurance_rate',
    '協会けんぽの都道府県別保険料額表をPythonでCSV化して取り込む定義',
    'SCRIPT',
    '/tmp/project-admin/import/health_insurance_rate_2026.csv',
    'PYTHON',
    '/app/storage/imports/scripts/convert_health_insurance_rate.py',
    '--year 2026 --prefecture 22 --input /tmp/project-admin/source/kyoukaikenpo_shizuoka_2026.pdf --output /tmp/project-admin/import/health_insurance_rate_2026.csv',
    'INSERT_ONLY',
    1,
    2,
    'UTF-8',
    ',',
    TRUE,
    'default',
    NOW(),
    NOW()
),
(
    'IMPORT_PENSION_INSURANCE_RATE',
    '厚生年金保険料額表取込',
    'pension_insurance_rate',
    '日本年金機構の厚生年金保険料額表をPythonでCSV化して取り込む定義',
    'SCRIPT',
    '/tmp/project-admin/import/pension_insurance_rate_2026.csv',
    'PYTHON',
    '/app/storage/imports/scripts/convert_pension_insurance_rate.py',
    '--year 2026 --input /tmp/project-admin/source/nenkin_pension_2026.pdf --output /tmp/project-admin/import/pension_insurance_rate_2026.csv',
    'INSERT_ONLY',
    1,
    2,
    'UTF-8',
    ',',
    TRUE,
    'default',
    NOW(),
    NOW()
),
(
    'IMPORT_EMPLOYMENT_INSURANCE_RATE',
    '雇用保険料率取込',
    'employment_insurance_rate',
    '厚労省の雇用保険料率をPythonでCSV化して取り込む定義',
    'SCRIPT',
    '/tmp/project-admin/import/employment_insurance_rate_2026.csv',
    'PYTHON',
    '/app/storage/imports/scripts/convert_employment_insurance_rate.py',
    '--year 2026 --input /tmp/project-admin/source/mhlw_employment_insurance_2026.pdf --output /tmp/project-admin/import/employment_insurance_rate_2026.csv',
    'INSERT_ONLY',
    1,
    2,
    'UTF-8',
    ',',
    TRUE,
    'default',
    NOW(),
    NOW()
),
(
    'IMPORT_STANDARD_MONTHLY_REMUNERATION',
    '標準報酬月額取込',
    'standard_monthly_remuneration',
    '健康保険・厚生年金の保険料額表から標準報酬月額等級を取り込む定義',
    'SCRIPT',
    '/tmp/project-admin/import/standard_monthly_remuneration_2026.csv',
    'PYTHON',
    '/app/storage/imports/scripts/convert_standard_monthly_remuneration.py',
    '--year 2026 --input /tmp/project-admin/source/social_insurance_2026.pdf --output /tmp/project-admin/import/standard_monthly_remuneration_2026.csv',
    'INSERT_ONLY',
    1,
    2,
    'UTF-8',
    ',',
    TRUE,
    'default',
    NOW(),
    NOW()
),
(
    'IMPORT_CHILD_CARE_SUPPORT_FUND',
    '子ども・子育て支援金取込',
    'child_care_support_fund',
    '保険料額表に含まれる子ども・子育て支援金を取り込む定義',
    'SCRIPT',
    '/tmp/project-admin/import/child_care_support_fund_2026.csv',
    'PYTHON',
    '/app/storage/imports/scripts/convert_child_care_support_fund.py',
    '--year 2026 --input /tmp/project-admin/source/social_insurance_2026.pdf --output /tmp/project-admin/import/child_care_support_fund_2026.csv',
    'INSERT_ONLY',
    1,
    2,
    'UTF-8',
    ',',
    TRUE,
    'default',
    NOW(),
    NOW()
),
(
    'IMPORT_RESIDENT_TAX',
    '住民税取込',
    'resident_tax',
    '市区町村から届く住民税通知書CSVを取り込む定義。自動取得対象外。',
    'SERVER_FILE',
    '/tmp/project-admin/import/resident_tax_2026.csv',
    'NONE',
    NULL,
    NULL,
    'UPSERT',
    1,
    2,
    'UTF-8',
    ',',
    TRUE,
    'default',
    NOW(),
    NOW()
);

UPDATE import_target
SET
    script_args = '--year 2026 --url https://www.nta.go.jp/publication/pamph/gensen/zeigakuhyo2026/data/01-07.xls --source /tmp/project-admin/source/nta_income_tax_monthly_2026.xls --output /tmp/project-admin/import/income_tax_table_2026.csv',
    fixed_file_path = '/tmp/project-admin/import/income_tax_table_2026.csv'
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

UPDATE import_target
SET
    table_name = 'insurance_rate_master',
    script_args = '--year 2026 --input /tmp/project-admin/source/kyoukaikenpo_shizuoka_2026.pdf --output /tmp/project-admin/import/health_insurance_rate_2026.csv'
WHERE target_code = 'IMPORT_HEALTH_INSURANCE_RATE';

UPDATE import_target
SET table_name = 'insurance_rate_master'
WHERE target_code IN (
    'IMPORT_PENSION_INSURANCE_RATE',
    'IMPORT_EMPLOYMENT_INSURANCE_RATE',
    'IMPORT_CHILD_CARE_SUPPORT_FUND'
);

UPDATE import_target
SET table_name = 'standard_salary_table'
WHERE target_code = 'IMPORT_STANDARD_MONTHLY_REMUNERATION';

UPDATE import_target
SET
    script_args = '--year 2026 --url https://www.kyoukaikenpo.or.jp/assets/R8_22shizuoka.pdf --input /tmp/project-admin/source/kyoukaikenpo_shizuoka_2026.pdf --output /tmp/project-admin/import/health_insurance_rate_2026.csv'
WHERE target_code = 'IMPORT_HEALTH_INSURANCE_RATE';