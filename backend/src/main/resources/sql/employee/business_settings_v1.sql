-- ProjectAdmin V1 業務設定（退職・締日・締め帳票）
-- MySQL 8.x / 既存データを削除しない追加DDL

CREATE TABLE IF NOT EXISTS employee_resignation_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_code VARCHAR(50) NOT NULL,
    dialog_title VARCHAR(200) NOT NULL,
    guidance_message VARCHAR(2000) NOT NULL,
    confirmation_message VARCHAR(500) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_resignation_setting_code
        UNIQUE (tenant_id, setting_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO employee_resignation_setting (
    setting_code, dialog_title, guidance_message, confirmation_message,
    tenant_id, created_at, updated_at
)
SELECT
    'DEFAULT',
    '退職処理',
    '退職日と確認項目を確認してから退職処理を実行してください。',
    '実行すると従業員の在籍状態が退職になります。',
    tenants.tenant_id,
    NOW(6),
    NOW(6)
FROM (
    SELECT DISTINCT tenant_id
    FROM employee
    WHERE deleted_at IS NULL
) tenants
WHERE NOT EXISTS (
    SELECT 1
    FROM employee_resignation_setting setting_row
    WHERE setting_row.tenant_id = tenants.tenant_id
      AND setting_row.setting_code = 'DEFAULT'
      AND setting_row.deleted_at IS NULL
);

-- 過去にUTF-8文字列がLatin-1として二重変換された既定文言だけを修復する。
-- 管理画面から利用者が変更した正常な文言は上書きしない。
UPDATE employee_resignation_setting
SET
    dialog_title = '退職処理',
    guidance_message = '退職日と確認項目を確認してから退職処理を実行してください。',
    confirmation_message = '実行すると従業員の在籍状態が退職になります。',
    updated_at = NOW(6)
WHERE setting_code = 'DEFAULT'
  AND deleted_at IS NULL
  AND CONVERT(
      BINARY CONVERT(dialog_title USING latin1)
      USING utf8mb4
  ) = '退職処理';

-- TODOは管理者が自由入力できるため、文字化け特有のLatin文字を含む項目だけ
-- 1段階逆変換する。通常の日本語・英数字は対象外とする。
UPDATE employee_resignation_checklist_master
SET
    name = CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4),
    updated_at = NOW(6)
WHERE deleted_at IS NULL
  AND name REGEXP '[ÃÂãäåæçèé]'
  AND CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4) IS NOT NULL;

UPDATE employee_resignation_checklist_master
SET
    description = CONVERT(
        BINARY CONVERT(description USING latin1)
        USING utf8mb4
    ),
    updated_at = NOW(6)
WHERE deleted_at IS NULL
  AND description IS NOT NULL
  AND description REGEXP '[ÃÂãäåæçèé]'
  AND CONVERT(
      BINARY CONVERT(description USING latin1)
      USING utf8mb4
  ) IS NOT NULL;

SET @checklist_duplicate_count := (
    SELECT COUNT(*)
    FROM (
        SELECT tenant_id, code
        FROM employee_resignation_checklist_master
        WHERE deleted_at IS NULL
        GROUP BY tenant_id, code
        HAVING COUNT(*) > 1
    ) duplicated
);

SET @checklist_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'employee_resignation_checklist_master'
      AND index_name = 'uk_employee_resignation_checklist_code'
);

SET @checklist_index_sql := IF(
    @checklist_index_exists = 0 AND @checklist_duplicate_count = 0,
    'ALTER TABLE employee_resignation_checklist_master ADD CONSTRAINT uk_employee_resignation_checklist_code UNIQUE (tenant_id, code)',
    'SELECT 1'
);
PREPARE checklist_index_statement FROM @checklist_index_sql;
EXECUTE checklist_index_statement;
DEALLOCATE PREPARE checklist_index_statement;

-- 現在の月次帳票を締め帳票の初期設定として登録する。
INSERT INTO monthly_closing_output_definition (
    output_type, output_code, execution_order, required_flag, active_flag,
    backup_retention_years, tenant_id, created_at, updated_at
)
SELECT
    'REPORT',
    preview.report_code,
    COALESCE(preview.display_order, 1),
    TRUE,
    preview.active_flag,
    CASE
        WHEN preview.report_code IN (
            'MONTHLY_PAY_SLIP',
            'MONTHLY_INVOICE',
            'MONTHLY_LABOR_COST_LIST'
        ) THEN 7
        ELSE NULL
    END,
    preview.tenant_id,
    NOW(6),
    NOW(6)
FROM operation_report_preview preview
WHERE preview.operation_type = 'MONTHLY'
  AND preview.deleted_at IS NULL
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at);
