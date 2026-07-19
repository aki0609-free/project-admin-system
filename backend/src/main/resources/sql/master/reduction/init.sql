DELETE FROM deduction_masters;

INSERT INTO deduction_masters (
    id,
    deduction_code,
    deduction_name,
    deduction_type,
    calculation_type,
    deduction_unit,
    detail_view_type,
    show_on_daily_statement,
    show_on_monthly_statement,
    carry_to_monthly_settlement,
    display_order,
    enabled,
    note,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
) VALUES

-- =========================
-- 所得税
-- =========================
(
    1,
    'INCOME_TAX',
    '所得税',
    'LEGAL',
    'VARIABLE',
    'PAYROLL',
    'INCOME_TAX',
    false,
    true,
    false,
    10,
    true,
    '給与所得税',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 住民税
-- =========================
(
    2,
    'RESIDENT_TAX',
    '住民税',
    'LEGAL',
    'VARIABLE',
    'MONTHLY',
    'RESIDENT_TAX',
    false,
    true,
    false,
    20,
    true,
    '住民税月額',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 健康保険
-- =========================
(
    3,
    'HEALTH_INSURANCE',
    '健康保険',
    'LEGAL',
    'VARIABLE',
    'MONTHLY',
    'INSURANCE_RATE',
    false,
    true,
    false,
    30,
    true,
    '健康保険料率',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 子ども子育て支援金
-- =========================
(
    4,
    'CHILD_SUPPORT',
    '子ども子育て支援金',
    'LEGAL',
    'VARIABLE',
    'MONTHLY',
    'INSURANCE_RATE',
    false,
    true,
    false,
    40,
    true,
    '子ども子育て支援金',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 厚生年金
-- =========================
(
    5,
    'WELFARE_PENSION',
    '厚生年金',
    'LEGAL',
    'VARIABLE',
    'MONTHLY',
    'INSURANCE_RATE',
    false,
    true,
    false,
    50,
    true,
    '厚生年金料率',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 雇用保険
-- =========================
(
    6,
    'EMPLOYMENT_INSURANCE',
    '雇用保険',
    'LEGAL',
    'VARIABLE',
    'MONTHLY',
    'INSURANCE_RATE',
    false,
    true,
    false,
    60,
    true,
    '雇用保険料率',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 前払い
-- =========================
(
    7,
    'ADVANCE_PAYMENT',
    '前払い',
    'COMPANY',
    'VARIABLE',
    'BOTH',
    'GENERIC',
    true,
    true,
    true,
    100,
    true,
    '前払い控除',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 寮費
-- =========================
(
    8,
    'DORMITORY_FEE',
    '寮費',
    'COMPANY',
    'FIXED',
    'MONTHLY',
    'GENERIC',
    false,
    true,
    true,
    110,
    true,
    '寮費控除',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 携帯貸出料
-- =========================
(
    9,
    'MOBILE_RENTAL',
    '携帯電話貸出料',
    'COMPANY',
    'FIXED',
    'MONTHLY',
    'GENERIC',
    false,
    true,
    true,
    120,
    true,
    '携帯電話貸出',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- WIFI
-- =========================
(
    10,
    'WIFI_FEE',
    'Wi-Fi使用料',
    'COMPANY',
    'FIXED',
    'MONTHLY',
    'GENERIC',
    false,
    true,
    true,
    130,
    true,
    'Wi-Fi利用料',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 貯金
-- =========================
(
    11,
    'SAVINGS',
    '貯金',
    'COMPANY',
    'VARIABLE',
    'BOTH',
    'GENERIC',
    true,
    true,
    true,
    140,
    true,
    '会社預り貯金',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 借入返済
-- =========================
(
    12,
    'LOAN_REPAYMENT',
    '借入金返済',
    'COMPANY',
    'VARIABLE',
    'BOTH',
    'GENERIC',
    true,
    true,
    true,
    150,
    true,
    '借入返済',
    'default',
    NOW(),
    NOW(),
    NULL
),

-- =========================
-- 法定預り金
-- =========================
(
    13,
    'LEGAL_DEPOSIT',
    '法定預り金',
    'LEGAL',
    'VARIABLE',
    'DAILY',
    'GENERIC',
    true,
    false,
    true,
    160,
    true,
    '日払い時預り',
    'default',
    NOW(),
    NOW(),
    NULL
);

UPDATE deduction_masters
SET detail_view_type = 'HEALTH_INSURANCE'
WHERE deduction_code = 'HEALTH_INSURANCE';

UPDATE deduction_masters
SET detail_view_type = 'PENSION'
WHERE deduction_code IN ('PENSION', 'WELFARE_PENSION');

UPDATE deduction_masters
SET detail_view_type = 'EMPLOYMENT_INSURANCE'
WHERE deduction_code = 'EMPLOYMENT_INSURANCE';

UPDATE deduction_masters
SET detail_view_type = 'GENERIC'
WHERE detail_view_type = 'INSURANCE_RATE';