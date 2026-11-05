-- 稼働中の給与制度設定を、汎用マスターの予約契約 PAYROLL_POLICY へ移行する。
-- 既に汎用データがあるテナントは再移行せず、以後は汎用マスターを正本とする。

INSERT INTO generic_master_definition (
    master_code, master_name, tab_name, placement, description,
    display_order, definition_status, active_flag, tenant_id,
    created_at, updated_at, deleted_at
)
SELECT
    'PAYROLL_POLICY',
    '会社・期間別給与制度',
    '給与制度設定',
    'BUSINESS_SETTINGS',
    '勤務日に有効な給与制度を選び、給与Ruleへ計算条件として渡します。適用期間は重複できません。',
    40,
    'PUBLISHED',
    TRUE,
    tenants.tenant_id,
    NOW(6), NOW(6), NULL
FROM (
    SELECT 'default' AS tenant_id
    UNION
    SELECT DISTINCT tenant_id
    FROM payroll_policy_setting
    WHERE deleted_at IS NULL
) tenants
WHERE NOT EXISTS (
    SELECT 1
    FROM generic_master_definition definition
    WHERE definition.tenant_id = tenants.tenant_id
      AND definition.master_code = 'PAYROLL_POLICY'
);

INSERT INTO generic_master_field_definition (
    definition_id, field_key, field_label, field_type,
    required_flag, list_visible_flag, searchable_flag, display_order,
    unit, placeholder, validation_pattern, options_json,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    definition.id,
    fields.field_key,
    fields.field_label,
    fields.field_type,
    fields.required_flag,
    fields.list_visible_flag,
    fields.searchable_flag,
    fields.display_order,
    fields.unit,
    fields.placeholder,
    NULL,
    fields.options_json,
    definition.tenant_id,
    NOW(6), NOW(6), NULL
FROM generic_master_definition definition
JOIN (
    SELECT 'EFFECTIVE_FROM' field_key, '適用開始日' field_label, 'DATE' field_type,
           TRUE required_flag, TRUE list_visible_flag, TRUE searchable_flag,
           10 display_order, NULL unit, NULL placeholder, NULL options_json
    UNION ALL SELECT 'EFFECTIVE_TO', '適用終了日', 'DATE', FALSE, TRUE, TRUE,
           20, NULL, '空欄は無期限', NULL
    UNION ALL SELECT 'WEEK_START_DAY', '週の起算曜日', 'SELECT', TRUE, TRUE, TRUE,
           30, NULL, NULL,
           '[{"title":"月曜日","value":"MONDAY"},{"title":"火曜日","value":"TUESDAY"},{"title":"水曜日","value":"WEDNESDAY"},{"title":"木曜日","value":"THURSDAY"},{"title":"金曜日","value":"FRIDAY"},{"title":"土曜日","value":"SATURDAY"},{"title":"日曜日","value":"SUNDAY"}]'
    UNION ALL SELECT 'WEEKLY_STATUTORY_HOURS', '週法定労働時間', 'NUMBER', TRUE, TRUE, TRUE,
           40, '時間', NULL, NULL
    UNION ALL SELECT 'MONTHLY_OVERTIME_THRESHOLD_HOURS', '月時間外割増切替', 'NUMBER', TRUE, TRUE, TRUE,
           50, '時間', NULL, NULL
    UNION ALL SELECT 'DAILY_STANDARD_HOURS', '日給の基準時間', 'NUMBER', TRUE, TRUE, TRUE,
           60, '時間', NULL, NULL
    UNION ALL SELECT 'OVERTIME_RATE', '時間外割増率', 'NUMBER', TRUE, TRUE, FALSE,
           70, '倍', NULL, NULL
    UNION ALL SELECT 'OVERTIME_OVER_THRESHOLD_RATE', '月基準超過後の割増率', 'NUMBER', TRUE, TRUE, FALSE,
           80, '倍', NULL, NULL
    UNION ALL SELECT 'NIGHT_PREMIUM_RATE', '深夜加算率', 'NUMBER', TRUE, TRUE, FALSE,
           90, '倍', NULL, NULL
    UNION ALL SELECT 'STATUTORY_HOLIDAY_RATE', '法定休日割増率', 'NUMBER', TRUE, TRUE, FALSE,
           100, '倍', NULL, NULL
    UNION ALL SELECT 'AMOUNT_ROUNDING_MODE', '円未満の端数処理', 'SELECT', TRUE, TRUE, TRUE,
           110, NULL, NULL,
           '[{"title":"四捨五入","value":"HALF_UP"},{"title":"切り上げ","value":"UP"},{"title":"切り捨て","value":"DOWN"}]'
) fields
WHERE definition.master_code = 'PAYROLL_POLICY'
  AND definition.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM generic_master_field_definition existing
      WHERE existing.tenant_id = definition.tenant_id
        AND existing.definition_id = definition.id
        AND existing.field_key = fields.field_key
  );

INSERT INTO generic_master_record (
    definition_id, values_json, active_flag, lock_version,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    definition.id,
    JSON_OBJECT(
        'EFFECTIVE_FROM', DATE_FORMAT(policy.effective_from, '%Y-%m-%d'),
        'EFFECTIVE_TO', IF(policy.effective_to IS NULL, NULL, DATE_FORMAT(policy.effective_to, '%Y-%m-%d')),
        'WEEK_START_DAY', policy.week_start_day,
        'WEEKLY_STATUTORY_HOURS', policy.weekly_statutory_hours,
        'MONTHLY_OVERTIME_THRESHOLD_HOURS', policy.monthly_overtime_threshold_hours,
        'OVERTIME_RATE', policy.overtime_rate,
        'OVERTIME_OVER_THRESHOLD_RATE', policy.overtime_over_threshold_rate,
        'NIGHT_PREMIUM_RATE', policy.night_premium_rate,
        'STATUTORY_HOLIDAY_RATE', policy.statutory_holiday_rate,
        'DAILY_STANDARD_HOURS', policy.daily_standard_hours,
        'AMOUNT_ROUNDING_MODE', policy.amount_rounding_mode
    ),
    policy.active_flag,
    0,
    policy.tenant_id,
    policy.created_at,
    policy.updated_at,
    policy.deleted_at
FROM payroll_policy_setting policy
JOIN generic_master_definition definition
  ON definition.tenant_id = policy.tenant_id
 AND definition.master_code = 'PAYROLL_POLICY'
 AND definition.deleted_at IS NULL
WHERE policy.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM generic_master_record existing
      WHERE existing.tenant_id = definition.tenant_id
        AND existing.definition_id = definition.id
        AND existing.deleted_at IS NULL
  );
