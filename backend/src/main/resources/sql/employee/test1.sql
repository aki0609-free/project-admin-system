INSERT INTO employee (
    tenant_id,
    employee_code,
    employee_name,
    employee_name_kana,
    gender,
    birth_date,
    hire_date,
    resign_date,
    employment_type,
    employment_status,
    phone,
    email,
    postal_code,
    address,
    active_flag,
    created_at,
    updated_at
) VALUES
(
    'default',
    'EMP001',
    '山田 太郎',
    'ヤマダ タロウ',
    'MALE',
    '1990-05-10',
    '2024-04-01',
    NULL,
    'FULL_TIME',
    'ACTIVE',
    '090-1111-1111',
    'yamada@example.com',
    '4160901',
    '静岡県富士市岩本1-1-1',
    true,
    NOW(),
    NOW()
),
(
    'default',
    'EMP002',
    '佐藤 花子',
    'サトウ ハナコ',
    'FEMALE',
    '1995-08-20',
    '2025-01-15',
    NULL,
    'CONTRACT',
    'ACTIVE',
    '090-2222-2222',
    'sato@example.com',
    '4170051',
    '静岡県富士市吉原2-2-2',
    true,
    NOW(),
    NOW()
),
(
    'default',
    'EMP003',
    '鈴木 一郎',
    'スズキ イチロウ',
    'MALE',
    '1985-12-01',
    '2023-06-01',
    NULL,
    'DAILY_WORKER',
    'ACTIVE',
    '090-3333-3333',
    'suzuki@example.com',
    '4101101',
    '静岡県裾野市岩波3-3-3',
    true,
    NOW(),
    NOW()
);

INSERT INTO employee_payroll_profile (
    tenant_id,
    employee_id,
    tax_category,
    tax_dependent_count,
    income_tax_calc_flag,
    resident_tax_calc_flag,
    resident_tax_monthly,
    employment_insurance_flag,
    social_insurance_flag,
    health_insurance_flag,
    pension_insurance_flag,
    care_insurance_flag,
    daily_pay_flag,
    commute_allowance_monthly,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    'KOU',
    1,
    true,
    true,
    12000,
    true,
    true,
    true,
    true,
    false,
    false,
    15000,
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP001';

INSERT INTO employee_payroll_profile (
    tenant_id,
    employee_id,
    tax_category,
    tax_dependent_count,
    income_tax_calc_flag,
    resident_tax_calc_flag,
    resident_tax_monthly,
    employment_insurance_flag,
    social_insurance_flag,
    health_insurance_flag,
    pension_insurance_flag,
    care_insurance_flag,
    daily_pay_flag,
    commute_allowance_monthly,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    'OTSU',
    0,
    true,
    true,
    8500,
    true,
    true,
    true,
    true,
    false,
    false,
    10000,
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP002';

INSERT INTO employee_payroll_profile (
    tenant_id,
    employee_id,
    tax_category,
    tax_dependent_count,
    income_tax_calc_flag,
    resident_tax_calc_flag,
    resident_tax_monthly,
    employment_insurance_flag,
    social_insurance_flag,
    health_insurance_flag,
    pension_insurance_flag,
    care_insurance_flag,
    daily_pay_flag,
    commute_allowance_monthly,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    'HEI',
    0,
    true,
    false,
    0,
    true,
    false,
    false,
    false,
    false,
    true,
    5000,
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP003';

INSERT INTO employee_contract (
    tenant_id,
    employee_id,
    contract_start_date,
    contract_end_date,
    renewal_flag,
    salary_type,
    monthly_salary,
    daily_wage,
    hourly_wage,
    standard_working_hours,
    note,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    '2024-04-01',
    NULL,
    false,
    'MONTHLY',
    320000,
    0,
    0,
    8,
    '現場責任者候補',
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP001';

INSERT INTO employee_contract (
    tenant_id,
    employee_id,
    contract_start_date,
    contract_end_date,
    renewal_flag,
    salary_type,
    monthly_salary,
    daily_wage,
    hourly_wage,
    standard_working_hours,
    note,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    '2025-01-15',
    '2026-01-14',
    true,
    'MONTHLY',
    260000,
    0,
    0,
    8,
    '1年契約社員',
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP002';

INSERT INTO employee_contract (
    tenant_id,
    employee_id,
    contract_start_date,
    contract_end_date,
    renewal_flag,
    salary_type,
    monthly_salary,
    daily_wage,
    hourly_wage,
    standard_working_hours,
    note,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    '2023-06-01',
    NULL,
    false,
    'DAILY',
    0,
    15000,
    0,
    8,
    '日払い対応',
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP003';
