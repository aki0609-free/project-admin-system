-- 退職ダイアログ文言を共通設定へ、退職時TODOを汎用マスターへ移行する。

INSERT INTO system_setting_definition (
    large_category_key, large_category_name,
    middle_category_key, middle_category_name,
    small_category_key, setting_scope, owner_tenant_id,
    setting_name, description, value_type, required_flag,
    validation_pattern, placeholder, display_order,
    sensitive_flag, active_flag, system_managed_flag,
    created_at, updated_at, deleted_at
)
VALUES
    ('BUSINESS', '業務共通設定', 'RESIGNATION_DIALOG', '退職ダイアログ',
     'DIALOG_TITLE', 'SYSTEM', '*', 'ダイアログタイトル',
     '従業員の退職処理ダイアログに表示するタイトルです。',
     'STRING', TRUE, NULL, '退職処理', 110, FALSE, TRUE, TRUE, NOW(6), NOW(6), NULL),
    ('BUSINESS', '業務共通設定', 'RESIGNATION_DIALOG', '退職ダイアログ',
     'GUIDANCE_MESSAGE', 'SYSTEM', '*', '案内文',
     '退職日と確認項目の上に表示する案内文です。',
     'STRING', TRUE, NULL, '退職日と確認項目を確認してから退職処理を実行してください。',
     120, FALSE, TRUE, TRUE, NOW(6), NOW(6), NULL),
    ('BUSINESS', '業務共通設定', 'RESIGNATION_DIALOG', '退職ダイアログ',
     'CONFIRMATION_MESSAGE', 'SYSTEM', '*', '警告見出し',
     '退職実行前に表示する確認メッセージです。',
     'STRING', TRUE, NULL, '実行すると従業員の在籍状態が退職になります。',
     130, FALSE, TRUE, TRUE, NOW(6), NOW(6), NULL)
ON DUPLICATE KEY UPDATE
    large_category_name = VALUES(large_category_name),
    middle_category_name = VALUES(middle_category_name),
    setting_name = VALUES(setting_name),
    description = VALUES(description),
    value_type = VALUES(value_type),
    required_flag = VALUES(required_flag),
    placeholder = VALUES(placeholder),
    display_order = VALUES(display_order),
    active_flag = VALUES(active_flag),
    system_managed_flag = VALUES(system_managed_flag),
    updated_at = NOW(6);

-- 既存の利用者設定を最初の1回だけ共通設定へ移す。
INSERT IGNORE INTO system_setting_value (
    definition_id, setting_value, tenant_id, created_at, updated_at, deleted_at
)
SELECT
    definition.id,
    CASE definition.small_category_key
        WHEN 'DIALOG_TITLE' THEN legacy.dialog_title
        WHEN 'GUIDANCE_MESSAGE' THEN legacy.guidance_message
        WHEN 'CONFIRMATION_MESSAGE' THEN legacy.confirmation_message
    END,
    legacy.tenant_id,
    legacy.created_at,
    legacy.updated_at,
    legacy.deleted_at
FROM employee_resignation_setting legacy
JOIN system_setting_definition definition
  ON definition.large_category_key = 'BUSINESS'
 AND definition.middle_category_key = 'RESIGNATION_DIALOG'
 AND definition.small_category_key IN (
     'DIALOG_TITLE', 'GUIDANCE_MESSAGE', 'CONFIRMATION_MESSAGE'
 )
WHERE legacy.setting_code = 'DEFAULT'
  AND legacy.deleted_at IS NULL;

-- 旧設定のないテナントにも、画面から編集できる既定値を配置する。
INSERT IGNORE INTO system_setting_value (
    definition_id, setting_value, tenant_id, created_at, updated_at, deleted_at
)
SELECT
    definition.id,
    CASE definition.small_category_key
        WHEN 'DIALOG_TITLE' THEN '退職処理'
        WHEN 'GUIDANCE_MESSAGE' THEN '退職日と確認項目を確認してから退職処理を実行してください。'
        WHEN 'CONFIRMATION_MESSAGE' THEN '実行すると従業員の在籍状態が退職になります。'
    END,
    tenants.tenant_id,
    NOW(6), NOW(6), NULL
FROM (
    SELECT 'default' tenant_id
    UNION SELECT DISTINCT tenant_id FROM users WHERE deleted_at IS NULL
    UNION SELECT DISTINCT tenant_id FROM employee WHERE deleted_at IS NULL
) tenants
JOIN system_setting_definition definition
  ON definition.large_category_key = 'BUSINESS'
 AND definition.middle_category_key = 'RESIGNATION_DIALOG'
 AND definition.small_category_key IN (
     'DIALOG_TITLE', 'GUIDANCE_MESSAGE', 'CONFIRMATION_MESSAGE'
 );

INSERT INTO generic_master_definition (
    master_code, master_name, tab_name, placement, description,
    display_order, definition_status, active_flag, tenant_id,
    created_at, updated_at, deleted_at
)
SELECT
    'RESIGNATION_CHECKLIST',
    '退職時TODO',
    '退職時TODO',
    'BUSINESS_SETTINGS',
    '退職処理で確認するTODOです。必須項目は確認しないと退職処理を実行できません。',
    10, 'PUBLISHED', TRUE, tenants.tenant_id,
    NOW(6), NOW(6), NULL
FROM (
    SELECT 'default' tenant_id
    UNION SELECT DISTINCT tenant_id FROM users WHERE deleted_at IS NULL
    UNION SELECT DISTINCT tenant_id FROM employee WHERE deleted_at IS NULL
) tenants
WHERE NOT EXISTS (
    SELECT 1 FROM generic_master_definition definition
    WHERE definition.tenant_id = tenants.tenant_id
      AND definition.master_code = 'RESIGNATION_CHECKLIST'
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
    NULL, fields.placeholder, fields.validation_pattern, NULL,
    definition.tenant_id, NOW(6), NOW(6), NULL
FROM generic_master_definition definition
JOIN (
    SELECT 'CODE' field_key, 'TODOコード' field_label, 'STRING' field_type,
           TRUE required_flag, TRUE searchable_flag, 10 display_order,
           '例: RETURN_CARD' placeholder, '^[A-Za-z0-9_-]+$' validation_pattern
    UNION ALL SELECT 'NAME', '項目名', 'STRING', TRUE, TRUE, 20, NULL, NULL
    UNION ALL SELECT 'DESCRIPTION', '説明', 'TEXT', FALSE, TRUE, 30, NULL, NULL
    UNION ALL SELECT 'REQUIRED_FLAG', '必須', 'BOOLEAN', TRUE, TRUE, 40, NULL, NULL
    UNION ALL SELECT 'DISPLAY_ORDER', '表示順', 'NUMBER', TRUE, FALSE, 50, NULL, NULL
) fields
WHERE definition.master_code = 'RESIGNATION_CHECKLIST'
  AND definition.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM generic_master_field_definition existing
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
        'CODE', legacy.code,
        'NAME', legacy.name,
        'DESCRIPTION', legacy.description,
        'REQUIRED_FLAG', legacy.required_flag,
        'DISPLAY_ORDER', legacy.display_order
    ),
    legacy.active_flag,
    0,
    legacy.tenant_id,
    legacy.created_at,
    legacy.updated_at,
    legacy.deleted_at
FROM employee_resignation_checklist_master legacy
JOIN generic_master_definition definition
  ON definition.tenant_id = legacy.tenant_id
 AND definition.master_code = 'RESIGNATION_CHECKLIST'
 AND definition.deleted_at IS NULL
WHERE legacy.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM generic_master_record existing
      WHERE existing.tenant_id = definition.tenant_id
        AND existing.definition_id = definition.id
        AND existing.deleted_at IS NULL
  );
