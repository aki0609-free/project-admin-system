-- =====================================================
-- Tax Import Column Definitions
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

-- =====================================================
-- 1. 所得税 源泉徴収税額表
-- table: income_tax_table
-- csv: year,minSalary,maxSalary,dependents,taxAmount
-- =====================================================

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'year', 'year', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       1, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'min_salary', 'minSalary', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       2, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'max_salary', 'maxSalary', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       3, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'dependents', 'dependents', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       4, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'tax_amount', 'taxAmount', 'INTEGER',
       TRUE, FALSE, FALSE, TRUE,
       NULL, NULL, TRUE,
       5, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_INCOME_TAX_TABLE';


-- =====================================================
-- 2. 健康保険料率
-- table: insurance_rate_master
-- csv: insuranceType,year,employeeRate,employerRate
-- =====================================================

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'insurance_type', 'insuranceType', 'STRING',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       1, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_HEALTH_INSURANCE_RATE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'year', 'year', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       2, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_HEALTH_INSURANCE_RATE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'employee_rate', 'employeeRate', 'DECIMAL',
       TRUE, FALSE, FALSE, TRUE,
       NULL, NULL, TRUE,
       3, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_HEALTH_INSURANCE_RATE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'employer_rate', 'employerRate', 'DECIMAL',
       FALSE, FALSE, TRUE, TRUE,
       NULL, NULL, TRUE,
       4, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_HEALTH_INSURANCE_RATE';


-- =====================================================
-- 3. 厚生年金
-- =====================================================

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'insurance_type', 'insuranceType', 'STRING',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       1, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_PENSION_INSURANCE_RATE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'year', 'year', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       2, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_PENSION_INSURANCE_RATE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'employee_rate', 'employeeRate', 'DECIMAL',
       TRUE, FALSE, FALSE, TRUE,
       NULL, NULL, TRUE,
       3, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_PENSION_INSURANCE_RATE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'employer_rate', 'employerRate', 'DECIMAL',
       FALSE, FALSE, TRUE, TRUE,
       NULL, NULL, TRUE,
       4, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_PENSION_INSURANCE_RATE';


-- =====================================================
-- 4. 雇用保険
-- =====================================================

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'insurance_type', 'insuranceType', 'STRING',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       1, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_EMPLOYMENT_INSURANCE_RATE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'year', 'year', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       2, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_EMPLOYMENT_INSURANCE_RATE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'employee_rate', 'employeeRate', 'DECIMAL',
       TRUE, FALSE, FALSE, TRUE,
       NULL, NULL, TRUE,
       3, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_EMPLOYMENT_INSURANCE_RATE';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'employer_rate', 'employerRate', 'DECIMAL',
       FALSE, FALSE, TRUE, TRUE,
       NULL, NULL, TRUE,
       4, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_EMPLOYMENT_INSURANCE_RATE';


-- =====================================================
-- 5. 子ども・子育て支援金
-- =====================================================

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'insurance_type', 'insuranceType', 'STRING',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       1, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_CHILD_CARE_SUPPORT_FUND';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'year', 'year', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       2, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_CHILD_CARE_SUPPORT_FUND';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'employee_rate', 'employeeRate', 'DECIMAL',
       TRUE, FALSE, FALSE, TRUE,
       NULL, NULL, TRUE,
       3, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_CHILD_CARE_SUPPORT_FUND';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'employer_rate', 'employerRate', 'DECIMAL',
       FALSE, FALSE, TRUE, TRUE,
       NULL, NULL, TRUE,
       4, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_CHILD_CARE_SUPPORT_FUND';


-- =====================================================
-- 6. 標準報酬月額
-- table: standard_salary_table
-- csv: minSalary,maxSalary,standardSalary
-- =====================================================

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'min_salary', 'minSalary', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       1, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_STANDARD_MONTHLY_REMUNERATION';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'max_salary', 'maxSalary', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       2, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_STANDARD_MONTHLY_REMUNERATION';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'standard_salary', 'standardSalary', 'INTEGER',
       TRUE, FALSE, FALSE, TRUE,
       NULL, NULL, TRUE,
       3, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_STANDARD_MONTHLY_REMUNERATION';


-- =====================================================
-- 7. 住民税
-- table: resident_tax_monthly
-- csv: employeeId,fiscalYear,month,taxAmount
-- =====================================================

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'employee_id', 'employeeId', 'LONG',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       1, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_RESIDENT_TAX';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'fiscal_year', 'fiscalYear', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       2, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_RESIDENT_TAX';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'month', 'month', 'INTEGER',
       TRUE, TRUE, FALSE, TRUE,
       NULL, NULL, TRUE,
       3, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_RESIDENT_TAX';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag,
    order_no, tenant_id, created_at, updated_at
)
SELECT id, 'tax_amount', 'taxAmount', 'INTEGER',
       TRUE, FALSE, FALSE, TRUE,
       NULL, NULL, TRUE,
       4, tenant_id, NOW(), NOW()
FROM import_target
WHERE target_code = 'IMPORT_RESIDENT_TAX';