-- 勤怠テストデータ
INSERT INTO employee_timesheet (
    tenant_id,
    employee_id,
    work_date,
    clock_in,
    clock_out,
    work_hours,
    overtime_hours,
    night_shift_hours,
    weekend_flag,
    approval_status,
    approval_comment,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    '2026-05-08',
    '08:00:00',
    '17:00:00',
    8.00,
    0.00,
    0.00,
    false,
    'APPROVED',
    '通常勤務',
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP001';

INSERT INTO employee_timesheet (
    tenant_id,
    employee_id,
    work_date,
    clock_in,
    clock_out,
    work_hours,
    overtime_hours,
    night_shift_hours,
    weekend_flag,
    approval_status,
    approval_comment,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    '2026-05-09',
    '08:30:00',
    '19:00:00',
    8.00,
    1.50,
    0.00,
    false,
    'PENDING',
    '残業あり',
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP002';

INSERT INTO employee_timesheet (
    tenant_id,
    employee_id,
    work_date,
    clock_in,
    clock_out,
    work_hours,
    overtime_hours,
    night_shift_hours,
    weekend_flag,
    approval_status,
    approval_comment,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    '2026-05-10',
    '21:00:00',
    '05:00:00',
    6.00,
    0.00,
    2.00,
    true,
    'PENDING',
    '夜勤・土日出勤',
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP003';

-- 従業員貸付テストデータ
INSERT INTO employee_loan (
    tenant_id,
    employee_id,
    principal,
    monthly_repayment,
    loan_date,
    repayment_start_date,
    active_flag,
    approval_status,
    approval_comment,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    100000,
    10000,
    '2026-04-01',
    '2026-05-25',
    true,
    'APPROVED',
    '前借分。給与控除予定。',
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP001';

INSERT INTO employee_loan (
    tenant_id,
    employee_id,
    principal,
    monthly_repayment,
    loan_date,
    repayment_start_date,
    active_flag,
    approval_status,
    approval_comment,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    50000,
    5000,
    '2026-04-15',
    '2026-05-25',
    true,
    'PENDING',
    '承認待ち',
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP003';

-- 従業員貯蓄テストデータ
INSERT INTO employee_saving (
    tenant_id,
    employee_id,
    percentage,
    min_salary_threshold,
    active_flag,
    apply_this_month,
    approval_status,
    approval_comment,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    10.00,
    180000,
    true,
    true,
    'APPROVED',
    '給与の10%を貯蓄',
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP001';

INSERT INTO employee_saving (
    tenant_id,
    employee_id,
    percentage,
    min_salary_threshold,
    active_flag,
    apply_this_month,
    approval_status,
    approval_comment,
    created_at,
    updated_at
)
SELECT
    'default',
    e.id,
    5.00,
    150000,
    true,
    false,
    'PENDING',
    '今月は未適用',
    NOW(),
    NOW()
FROM employee e
WHERE e.employee_code = 'EMP002';
