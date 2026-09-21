-- ProjectAdminSystem V1 / Fuyo
-- 日報・月次給与で利用する手当マスターの非破壊初期化。
-- 旧init.sqlは全件削除を伴うため、既存環境にはこのSQLを適用する。

SET NAMES utf8mb4;

INSERT INTO allowance_masters (
    allowance_code,
    allowance_name,
    allowance_type,
    calculation_type,
    allowance_unit,
    detail_view_type,
    rule_name,
    default_amount,
    allow_manual_input,
    min_amount,
    max_amount,
    taxable,
    show_on_daily_statement,
    show_on_monthly_statement,
    display_order,
    enabled,
    note,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'DRIVER_ALLOWANCE',
    '運転手当',
    'COMPANY',
    'AUTO',
    'BOTH',
    'NONE',
    'DAILY_DRIVER_ALLOWANCE',
    0,
    FALSE,
    0,
    NULL,
    TRUE,
    TRUE,
    TRUE,
    20,
    TRUE,
    '社員手配時：走行距離×15円＋同乗者数×200円（単価はRuleパラメータで変更可能）',
    'default',
    NOW(6),
    NOW(6),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM allowance_masters existing
    WHERE existing.allowance_code = 'DRIVER_ALLOWANCE'
      AND existing.deleted_at IS NULL
);

UPDATE allowance_masters
SET allowance_name = '運転手当',
    allowance_type = 'COMPANY',
    calculation_type = 'AUTO',
    allowance_unit = 'BOTH',
    detail_view_type = 'NONE',
    rule_name = 'DAILY_DRIVER_ALLOWANCE',
    default_amount = 0,
    allow_manual_input = FALSE,
    min_amount = 0,
    taxable = TRUE,
    show_on_daily_statement = TRUE,
    show_on_monthly_statement = TRUE,
    display_order = 20,
    enabled = TRUE,
    note = '社員手配時：走行距離×15円＋同乗者数×200円（単価はRuleパラメータで変更可能）',
    updated_at = NOW(6)
WHERE allowance_code = 'DRIVER_ALLOWANCE'
  AND deleted_at IS NULL;

INSERT INTO allowance_masters (
    allowance_code,
    allowance_name,
    allowance_type,
    calculation_type,
    allowance_unit,
    detail_view_type,
    rule_name,
    default_amount,
    allow_manual_input,
    min_amount,
    max_amount,
    taxable,
    show_on_daily_statement,
    show_on_monthly_statement,
    display_order,
    enabled,
    note,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'MANAGEMENT_ALLOWANCE',
    '管理手当',
    'COMPANY',
    'MANUAL',
    'BOTH',
    'NONE',
    NULL,
    0,
    TRUE,
    0,
    NULL,
    TRUE,
    TRUE,
    TRUE,
    30,
    TRUE,
    '日報で手入力し、月次では日次確定額を集計する',
    'default',
    NOW(6),
    NOW(6),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM allowance_masters existing
    WHERE existing.allowance_code = 'MANAGEMENT_ALLOWANCE'
      AND existing.deleted_at IS NULL
);

UPDATE allowance_masters
SET allowance_name = '管理手当',
    allowance_type = 'COMPANY',
    calculation_type = 'MANUAL',
    allowance_unit = 'BOTH',
    detail_view_type = 'NONE',
    rule_name = NULL,
    default_amount = 0,
    allow_manual_input = TRUE,
    min_amount = 0,
    taxable = TRUE,
    show_on_daily_statement = TRUE,
    show_on_monthly_statement = TRUE,
    display_order = 30,
    enabled = TRUE,
    note = '日報で手入力し、月次では日次確定額を集計する',
    updated_at = NOW(6)
WHERE allowance_code = 'MANAGEMENT_ALLOWANCE'
  AND deleted_at IS NULL;
