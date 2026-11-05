-- 給与締日・支払日設定を汎用マスターへ移行する。
-- closing_settingは締め処理・SQL帳票向けの互換投影として残す。

INSERT INTO generic_master_definition (
    master_code, master_name, tab_name, placement, description,
    display_order, definition_status, active_flag, tenant_id,
    created_at, updated_at, deleted_at
)
SELECT
    'PAYROLL_CLOSING_SETTING',
    '給与締日・支払日設定',
    '締日設定',
    'BUSINESS_SETTINGS',
    '給与の締日と支払日を設定します。有効な設定は1件だけ登録できます。',
    20, 'PUBLISHED', TRUE, tenants.tenant_id,
    NOW(6), NOW(6), NULL
FROM (
    SELECT 'default' tenant_id
    UNION SELECT DISTINCT tenant_id FROM users WHERE deleted_at IS NULL
    UNION SELECT DISTINCT tenant_id FROM employee WHERE deleted_at IS NULL
) tenants
WHERE NOT EXISTS (
    SELECT 1 FROM generic_master_definition definition
    WHERE definition.tenant_id = tenants.tenant_id
      AND definition.master_code = 'PAYROLL_CLOSING_SETTING'
);

INSERT INTO generic_master_field_definition (
    definition_id, field_key, field_label, field_type,
    required_flag, list_visible_flag, searchable_flag, display_order,
    unit, placeholder, validation_pattern, options_json,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    definition.id, fields.field_key, fields.field_label, fields.field_type,
    fields.required_flag, TRUE, fields.searchable_flag, fields.display_order,
    fields.unit, fields.placeholder, fields.validation_pattern, fields.options_json,
    definition.tenant_id, NOW(6), NOW(6), NULL
FROM generic_master_definition definition
JOIN (
    SELECT 'SETTING_CODE' field_key, '設定コード' field_label, 'STRING' field_type,
           TRUE required_flag, TRUE searchable_flag, 10 display_order,
           NULL unit, 'PAYROLL' placeholder, '^PAYROLL$' validation_pattern,
           NULL options_json
    UNION ALL SELECT 'CLOSING_MONTH_OFFSET', '締日の対象月', 'SELECT',
           TRUE, TRUE, 20, NULL, NULL, NULL,
           '[{"title":"前月","value":-1},{"title":"当月","value":0},{"title":"翌月","value":1},{"title":"翌々月","value":2}]'
    UNION ALL SELECT 'CLOSING_DAY_TYPE', '締日の種別', 'SELECT',
           TRUE, TRUE, 30, NULL, NULL, NULL,
           '[{"title":"日付指定","value":"DAY_OF_MONTH"},{"title":"末日","value":"END_OF_MONTH"}]'
    UNION ALL SELECT 'CLOSING_DAY_VALUE', '締日', 'NUMBER',
           FALSE, TRUE, 40, '日', '末日は空欄', NULL, NULL
    UNION ALL SELECT 'PAYMENT_MONTH_OFFSET', '支払日の対象月', 'SELECT',
           TRUE, TRUE, 50, NULL, NULL, NULL,
           '[{"title":"前月","value":-1},{"title":"当月","value":0},{"title":"翌月","value":1},{"title":"翌々月","value":2}]'
    UNION ALL SELECT 'PAYMENT_DAY_TYPE', '支払日の種別', 'SELECT',
           TRUE, TRUE, 60, NULL, NULL, NULL,
           '[{"title":"日付指定","value":"DAY_OF_MONTH"},{"title":"末日","value":"END_OF_MONTH"}]'
    UNION ALL SELECT 'PAYMENT_DAY_VALUE', '支払日', 'NUMBER',
           FALSE, TRUE, 70, '日', '末日は空欄', NULL, NULL
) fields
WHERE definition.master_code = 'PAYROLL_CLOSING_SETTING'
  AND definition.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM generic_master_field_definition existing
      WHERE existing.tenant_id = definition.tenant_id
        AND existing.definition_id = definition.id
        AND existing.field_key = fields.field_key
  );

-- 現在有効な既存設定を初回だけ汎用レコードへ移行する。
INSERT INTO generic_master_record (
    definition_id, values_json, active_flag, lock_version,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    definition.id,
    JSON_OBJECT(
        'SETTING_CODE', legacy.setting_code,
        'CLOSING_DAY_TYPE', legacy.closing_day_type,
        'CLOSING_DAY_VALUE', legacy.closing_day_value,
        'CLOSING_MONTH_OFFSET', legacy.closing_month_offset,
        'PAYMENT_DAY_TYPE', legacy.payment_day_type,
        'PAYMENT_DAY_VALUE', legacy.payment_day_value,
        'PAYMENT_MONTH_OFFSET', legacy.payment_month_offset
    ),
    TRUE, 0, legacy.tenant_id,
    legacy.created_at, legacy.updated_at, NULL
FROM closing_setting legacy
JOIN generic_master_definition definition
  ON definition.tenant_id = legacy.tenant_id
 AND definition.master_code = 'PAYROLL_CLOSING_SETTING'
 AND definition.deleted_at IS NULL
WHERE legacy.setting_code = 'PAYROLL'
  AND legacy.active_flag = TRUE
  AND legacy.deleted_at IS NULL
  AND legacy.id = (
      SELECT MAX(latest.id)
      FROM closing_setting latest
      WHERE latest.tenant_id = legacy.tenant_id
        AND latest.setting_code = 'PAYROLL'
        AND latest.active_flag = TRUE
        AND latest.deleted_at IS NULL
  )
  AND NOT EXISTS (
      SELECT 1 FROM generic_master_record existing
      WHERE existing.tenant_id = definition.tenant_id
        AND existing.definition_id = definition.id
        AND existing.deleted_at IS NULL
  );
