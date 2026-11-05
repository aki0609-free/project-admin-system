-- システム共通設定を、定義マスターとテナント別の値に分離して管理する。

CREATE TABLE IF NOT EXISTS system_setting_definition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    large_category_key VARCHAR(100) NOT NULL,
    large_category_name VARCHAR(200) NOT NULL,
    middle_category_key VARCHAR(100) NOT NULL,
    middle_category_name VARCHAR(200) NOT NULL,
    small_category_key VARCHAR(100) NOT NULL,
    setting_scope VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    owner_tenant_id VARCHAR(255) NOT NULL DEFAULT '*',
    setting_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NULL,
    value_type VARCHAR(30) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT FALSE,
    validation_pattern VARCHAR(1000) NULL,
    placeholder VARCHAR(500) NULL,
    display_order INT NOT NULL DEFAULT 0,
    sensitive_flag BOOLEAN NOT NULL DEFAULT FALSE,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    system_managed_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_system_setting_definition_key
        UNIQUE (
            setting_scope, owner_tenant_id,
            large_category_key, middle_category_key, small_category_key
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @definition_scope_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'system_setting_definition'
      AND column_name = 'setting_scope'
);
SET @definition_scope_ddl = IF(
    @definition_scope_exists = 0,
    'ALTER TABLE system_setting_definition ADD COLUMN setting_scope VARCHAR(20) NOT NULL DEFAULT ''SYSTEM'' AFTER small_category_key',
    'SELECT 1'
);
PREPARE definition_scope_statement FROM @definition_scope_ddl;
EXECUTE definition_scope_statement;
DEALLOCATE PREPARE definition_scope_statement;

SET @definition_owner_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'system_setting_definition'
      AND column_name = 'owner_tenant_id'
);
SET @definition_owner_ddl = IF(
    @definition_owner_exists = 0,
    'ALTER TABLE system_setting_definition ADD COLUMN owner_tenant_id VARCHAR(255) NOT NULL DEFAULT ''*'' AFTER setting_scope',
    'SELECT 1'
);
PREPARE definition_owner_statement FROM @definition_owner_ddl;
EXECUTE definition_owner_statement;
DEALLOCATE PREPARE definition_owner_statement;

SET @definition_managed_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'system_setting_definition'
      AND column_name = 'system_managed_flag'
);
SET @definition_managed_ddl = IF(
    @definition_managed_exists = 0,
    'ALTER TABLE system_setting_definition ADD COLUMN system_managed_flag BOOLEAN NOT NULL DEFAULT FALSE AFTER active_flag',
    'SELECT 1'
);
PREPARE definition_managed_statement FROM @definition_managed_ddl;
EXECUTE definition_managed_statement;
DEALLOCATE PREPARE definition_managed_statement;

SET @definition_deleted_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'system_setting_definition'
      AND column_name = 'deleted_at'
);
SET @definition_deleted_ddl = IF(
    @definition_deleted_exists = 0,
    'ALTER TABLE system_setting_definition ADD COLUMN deleted_at TIMESTAMP(6) NULL AFTER updated_at',
    'SELECT 1'
);
PREPARE definition_deleted_statement FROM @definition_deleted_ddl;
EXECUTE definition_deleted_statement;
DEALLOCATE PREPARE definition_deleted_statement;

SET @definition_key_index_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'system_setting_definition'
      AND index_name = 'uk_system_setting_definition_key'
);
SET @drop_definition_key_index = IF(
    @definition_key_index_exists > 0,
    'ALTER TABLE system_setting_definition DROP INDEX uk_system_setting_definition_key',
    'SELECT 1'
);
PREPARE drop_definition_key_statement FROM @drop_definition_key_index;
EXECUTE drop_definition_key_statement;
DEALLOCATE PREPARE drop_definition_key_statement;

UPDATE system_setting_definition
SET setting_scope = 'SYSTEM'
WHERE setting_scope IS NULL OR setting_scope = '';

UPDATE system_setting_definition
SET owner_tenant_id = '*'
WHERE owner_tenant_id IS NULL OR owner_tenant_id = '';

UPDATE system_setting_definition
SET system_managed_flag = FALSE
WHERE system_managed_flag IS NULL;

ALTER TABLE system_setting_definition
    MODIFY COLUMN setting_scope VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    MODIFY COLUMN owner_tenant_id VARCHAR(255) NOT NULL DEFAULT '*',
    MODIFY COLUMN system_managed_flag BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS system_setting_value (
    id BIGINT NOT NULL AUTO_INCREMENT,
    definition_id BIGINT NOT NULL,
    setting_value TEXT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_system_setting_value_tenant_definition
        UNIQUE (tenant_id, definition_id),
    CONSTRAINT fk_system_setting_value_definition
        FOREIGN KEY (definition_id) REFERENCES system_setting_definition (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Hibernate更新が先行してNULL列が追加された環境でも、既存値を保持して定義を一意化する。
DROP TEMPORARY TABLE IF EXISTS tmp_system_setting_definition_duplicate;
CREATE TEMPORARY TABLE tmp_system_setting_definition_duplicate AS
SELECT
    duplicate.id AS duplicate_id,
    MIN(canonical.id) AS canonical_id
FROM system_setting_definition duplicate
JOIN system_setting_definition canonical
  ON canonical.setting_scope = duplicate.setting_scope
 AND canonical.owner_tenant_id = duplicate.owner_tenant_id
 AND canonical.large_category_key = duplicate.large_category_key
 AND canonical.middle_category_key = duplicate.middle_category_key
 AND canonical.small_category_key = duplicate.small_category_key
GROUP BY duplicate.id
HAVING duplicate.id <> MIN(canonical.id);

INSERT INTO system_setting_value (
    definition_id, setting_value, tenant_id, created_at, updated_at, deleted_at
)
SELECT
    duplicate.canonical_id,
    value.setting_value,
    value.tenant_id,
    value.created_at,
    value.updated_at,
    value.deleted_at
FROM system_setting_value value
JOIN tmp_system_setting_definition_duplicate duplicate
  ON duplicate.duplicate_id = value.definition_id
ON DUPLICATE KEY UPDATE
    setting_value = VALUES(setting_value),
    updated_at = VALUES(updated_at),
    deleted_at = VALUES(deleted_at);

DELETE value
FROM system_setting_value value
JOIN tmp_system_setting_definition_duplicate duplicate
  ON duplicate.duplicate_id = value.definition_id;

DELETE definition
FROM system_setting_definition definition
JOIN tmp_system_setting_definition_duplicate duplicate
  ON duplicate.duplicate_id = definition.id;

DROP TEMPORARY TABLE tmp_system_setting_definition_duplicate;

ALTER TABLE system_setting_definition
    ADD CONSTRAINT uk_system_setting_definition_key UNIQUE (
        setting_scope, owner_tenant_id,
        large_category_key, middle_category_key, small_category_key
    );

INSERT INTO system_setting_definition (
    large_category_key, large_category_name,
    middle_category_key, middle_category_name,
    small_category_key, setting_scope, owner_tenant_id, setting_name, description,
    value_type, required_flag, validation_pattern, placeholder,
    display_order, sensitive_flag, active_flag, system_managed_flag,
    created_at, updated_at, deleted_at
)
VALUES
    (
        'SYSTEM', 'システム共通設定',
        'SUPPORT_LINK', 'サポートリンク',
        'MANUAL_URL', 'SYSTEM', '*', 'マニュアルURL',
        'ヘッダー右上のユーザーメニューから開くマニュアルのURLです。',
        'URL', TRUE, NULL, 'https://example.com/manual',
        10, FALSE, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
    ),
    (
        'SYSTEM', 'システム共通設定',
        'SUPPORT_LINK', 'サポートリンク',
        'INCIDENT_REPORT_URL', 'SYSTEM', '*', 'インシデント報告URL',
        'ヘッダー右上のユーザーメニューから開く障害報告フォームのURLです。',
        'URL', TRUE, NULL, 'https://example.com/incident',
        20, FALSE, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
    )
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
    updated_at = CURRENT_TIMESTAMP(6);

-- 旧サポートリンク専用テーブルが存在する環境では、その値を共通設定へ移行する。
SET @legacy_support_table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'external_support_link_setting'
);

SET @migrate_manual_url = IF(
    @legacy_support_table_exists > 0,
    'INSERT INTO system_setting_value (definition_id, setting_value, tenant_id, created_at, updated_at, deleted_at) SELECT definition.id, legacy.manual_url, legacy.tenant_id, legacy.created_at, legacy.updated_at, legacy.deleted_at FROM external_support_link_setting legacy JOIN system_setting_definition definition ON definition.large_category_key = ''SYSTEM'' AND definition.middle_category_key = ''SUPPORT_LINK'' AND definition.small_category_key = ''MANUAL_URL'' ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = VALUES(updated_at), deleted_at = VALUES(deleted_at)',
    'SELECT 1'
);
PREPARE migrate_manual_url_statement FROM @migrate_manual_url;
EXECUTE migrate_manual_url_statement;
DEALLOCATE PREPARE migrate_manual_url_statement;

SET @migrate_incident_url = IF(
    @legacy_support_table_exists > 0,
    'INSERT INTO system_setting_value (definition_id, setting_value, tenant_id, created_at, updated_at, deleted_at) SELECT definition.id, legacy.incident_report_url, legacy.tenant_id, legacy.created_at, legacy.updated_at, legacy.deleted_at FROM external_support_link_setting legacy JOIN system_setting_definition definition ON definition.large_category_key = ''SYSTEM'' AND definition.middle_category_key = ''SUPPORT_LINK'' AND definition.small_category_key = ''INCIDENT_REPORT_URL'' ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = VALUES(updated_at), deleted_at = VALUES(deleted_at)',
    'SELECT 1'
);
PREPARE migrate_incident_url_statement FROM @migrate_incident_url;
EXECUTE migrate_incident_url_statement;
DEALLOCATE PREPARE migrate_incident_url_statement;

INSERT INTO system_setting_value (
    definition_id, setting_value, tenant_id, created_at, updated_at, deleted_at
)
SELECT
    definition.id,
    CONCAT(
        'https://projectadmin1215.atlassian.net/wiki/spaces/',
        '~712020d0db24f25d734730b24dfb1508d24613/folder/19464193'
    ),
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM system_setting_definition definition
WHERE definition.large_category_key = 'SYSTEM'
  AND definition.middle_category_key = 'SUPPORT_LINK'
  AND definition.small_category_key = 'MANUAL_URL'
  AND NOT EXISTS (
      SELECT 1 FROM system_setting_value value
      WHERE value.tenant_id = 'default'
        AND value.definition_id = definition.id
  );

INSERT INTO system_setting_value (
    definition_id, setting_value, tenant_id, created_at, updated_at, deleted_at
)
SELECT
    definition.id,
    CONCAT(
        'https://projectadmin1215.atlassian.net/jira/software/projects/FUYO/form/1',
        '?atlOrigin=eyJpIjoiYTFkN2E2NWU2YWYwNGQ2ODk4MDRmMTliM2JkMjQ3YjgiLCJwIjoiaiJ9'
    ),
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM system_setting_definition definition
WHERE definition.large_category_key = 'SYSTEM'
  AND definition.middle_category_key = 'SUPPORT_LINK'
  AND definition.small_category_key = 'INCIDENT_REPORT_URL'
  AND NOT EXISTS (
      SELECT 1 FROM system_setting_value value
      WHERE value.tenant_id = 'default'
        AND value.definition_id = definition.id
  );

-- 専用テーブルは移行後に削除し、保存先を一本化する。
DROP TABLE IF EXISTS external_support_link_setting;
