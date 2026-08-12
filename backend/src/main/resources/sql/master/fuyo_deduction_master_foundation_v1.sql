-- ProjectAdminSystem V1 / Fuyo
-- 専用基盤で算出する前払い・貯金・借入返済を重複登録せず、
-- 控除マスターから直接利用する会社固有控除だけを保証する。

SET NAMES utf8mb4;

INSERT INTO deduction_masters (
    deduction_code,
    deduction_name,
    deduction_type,
    calculation_type,
    rule_name,
    default_amount,
    allow_manual_input,
    min_amount,
    max_amount,
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
)
SELECT
    seed.deduction_code,
    seed.deduction_name,
    'LEGAL',
    'MANUAL',
    NULL,
    NULL,
    TRUE,
    0,
    10000000,
    'DAILY',
    'NONE',
    TRUE,
    FALSE,
    TRUE,
    160,
    TRUE,
    '日報で手入力し、月次締めで未返金残高を精算する',
    'default',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM (
    SELECT 'LEGAL_DEPOSIT' AS deduction_code,
           '法定預り金' AS deduction_name
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM deduction_masters existing
    WHERE existing.tenant_id = 'default'
      AND existing.deduction_code = seed.deduction_code
      AND existing.deleted_at IS NULL
);

INSERT INTO deduction_masters (
    deduction_code,
    deduction_name,
    deduction_type,
    calculation_type,
    rule_name,
    default_amount,
    allow_manual_input,
    min_amount,
    max_amount,
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
)
SELECT
    seed.deduction_code,
    seed.deduction_name,
    'COMPANY',
    'MANUAL',
    NULL,
    0,
    TRUE,
    0,
    10000000,
    'MONTHLY',
    'NONE',
    FALSE,
    TRUE,
    TRUE,
    130,
    TRUE,
    '締め期間内の確定済み控除取引を月次集計する',
    'default',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM (
    SELECT 'WIFI_FEE' AS deduction_code,
           'Wi-Fi使用料' AS deduction_name
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM deduction_masters existing
    WHERE existing.tenant_id = 'default'
      AND existing.deduction_code = seed.deduction_code
      AND existing.deleted_at IS NULL
);

-- 旧初期データのJava Enum非互換値を、V1の確定仕様へ補正する。
UPDATE deduction_masters
SET deduction_name = '法定預り金',
    deduction_type = 'LEGAL',
    calculation_type = 'MANUAL',
    rule_name = NULL,
    default_amount = NULL,
    allow_manual_input = TRUE,
    min_amount = 0,
    max_amount = 10000000,
    deduction_unit = 'DAILY',
    detail_view_type = 'NONE',
    show_on_daily_statement = TRUE,
    show_on_monthly_statement = FALSE,
    carry_to_monthly_settlement = TRUE,
    display_order = 160,
    enabled = TRUE,
    note = '日報で手入力し、月次締めで未返金残高を精算する',
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL
WHERE tenant_id = 'default'
  AND deduction_code = 'LEGAL_DEPOSIT';

UPDATE deduction_masters
SET deduction_name = 'Wi-Fi使用料',
    deduction_type = 'COMPANY',
    calculation_type = 'MANUAL',
    rule_name = NULL,
    default_amount = 0,
    allow_manual_input = TRUE,
    min_amount = 0,
    max_amount = 10000000,
    deduction_unit = 'MONTHLY',
    detail_view_type = 'NONE',
    show_on_daily_statement = FALSE,
    show_on_monthly_statement = TRUE,
    carry_to_monthly_settlement = TRUE,
    display_order = 130,
    enabled = TRUE,
    note = '締め期間内の確定済み控除取引を月次集計する',
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL
WHERE tenant_id = 'default'
  AND deduction_code = 'WIFI_FEE';

-- 旧データの法定控除にだけ存在する非互換値をAUTOへ移行する。
UPDATE deduction_masters
SET calculation_type = 'AUTO',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE tenant_id = 'default'
  AND deduction_code IN (
      'INCOME_TAX',
      'RESIDENT_TAX',
      'HEALTH_INSURANCE',
      'CHILD_SUPPORT',
      'WELFARE_PENSION',
      'EMPLOYMENT_INSURANCE'
  )
  AND (calculation_type IS NULL OR calculation_type NOT IN ('MANUAL', 'FIXED', 'AUTO'));

-- 旧初期データに存在する専用基盤由来の表示メタデータは削除しない。
-- 日報の専用入力欄と二重表示しないよう月次専用へ寄せ、Enum非互換値を解消する。
UPDATE deduction_masters
SET calculation_type = 'MANUAL',
    deduction_unit = 'MONTHLY',
    detail_view_type = 'NONE',
    show_on_daily_statement = FALSE,
    show_on_monthly_statement = TRUE,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE tenant_id = 'default'
  AND deduction_code IN ('ADVANCE_PAYMENT', 'SAVINGS', 'LOAN_REPAYMENT')
  AND deleted_at IS NULL;

-- Java Enumへ変換できない旧値を残さない。
UPDATE deduction_masters
SET calculation_type = 'MANUAL',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE tenant_id = 'default'
  AND (calculation_type IS NULL OR calculation_type NOT IN ('MANUAL', 'FIXED', 'AUTO'))
  AND deleted_at IS NULL;
