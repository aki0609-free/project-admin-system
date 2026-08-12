-- ProjectAdminSystem V1
-- 顧客・取引管理・従業員・日報をCSV化し、既存バックアップ基盤からZIP取得する。
-- 対象テーブルへ将来カラムが追加された場合は、再適用時に定義を自動同期する。

SET NAMES utf8mb4;

INSERT INTO backup_target (
    target_code,
    target_name,
    table_name,
    description,
    backup_enabled,
    active_flag,
    output_mode,
    output_dir,
    file_name_pattern,
    zip_required,
    include_header,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
) VALUES
(
    'BACKUP_CUSTOMERS',
    '顧客',
    'customers',
    '顧客マスターの全カラムをCSVでバックアップする',
    TRUE,
    TRUE,
    'DOWNLOAD',
    NULL,
    'customers_{timestamp}.csv',
    TRUE,
    TRUE,
    'default',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
),
(
    'BACKUP_CUSTOMER_TRANSACTIONS',
    '顧客取引管理',
    'customer_transactions',
    '顧客取引管理の全カラムをCSVでバックアップする',
    TRUE,
    TRUE,
    'DOWNLOAD',
    NULL,
    'customer_transactions_{timestamp}.csv',
    TRUE,
    TRUE,
    'default',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
),
(
    'BACKUP_EMPLOYEES',
    '従業員',
    'employee',
    '従業員基本情報の全カラムをCSVでバックアップする',
    TRUE,
    TRUE,
    'DOWNLOAD',
    NULL,
    'employees_{timestamp}.csv',
    TRUE,
    TRUE,
    'default',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
),
(
    'BACKUP_DAILY_REPORTS',
    '日報',
    'daily_report',
    '日報本体の全カラムをCSVでバックアップする',
    TRUE,
    TRUE,
    'DOWNLOAD',
    NULL,
    'daily_reports_{timestamp}.csv',
    TRUE,
    TRUE,
    'default',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
)
ON DUPLICATE KEY UPDATE
    target_name = VALUES(target_name),
    table_name = VALUES(table_name),
    description = VALUES(description),
    backup_enabled = VALUES(backup_enabled),
    active_flag = VALUES(active_flag),
    output_mode = VALUES(output_mode),
    output_dir = VALUES(output_dir),
    file_name_pattern = VALUES(file_name_pattern),
    zip_required = VALUES(zip_required),
    include_header = VALUES(include_header),
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL;

-- 4対象はシステム管理定義とし、現在のテーブル定義へ同期する。
DELETE backup_column
FROM backup_column
JOIN backup_target target
  ON target.id = backup_column.target_id
WHERE target.tenant_id = 'default'
  AND target.target_code IN (
      'BACKUP_CUSTOMERS',
      'BACKUP_CUSTOMER_TRANSACTIONS',
      'BACKUP_EMPLOYEES',
      'BACKUP_DAILY_REPORTS'
  );

INSERT INTO backup_column (
    target_id,
    column_name,
    csv_header_name,
    data_type,
    export_flag,
    order_no,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    target.id,
    source_column.column_name,
    source_column.column_name,
    CASE
        WHEN source_column.data_type IN ('tinyint', 'bit', 'boolean') THEN 'BOOLEAN'
        WHEN source_column.data_type IN ('smallint', 'mediumint', 'int', 'integer') THEN 'INTEGER'
        WHEN source_column.data_type = 'bigint' THEN 'LONG'
        WHEN source_column.data_type IN (
            'decimal', 'numeric', 'float', 'double', 'real'
        ) THEN 'DECIMAL'
        WHEN source_column.data_type = 'date' THEN 'DATE'
        WHEN source_column.data_type IN ('datetime', 'timestamp') THEN 'DATETIME'
        ELSE 'STRING'
    END,
    TRUE,
    source_column.ordinal_position,
    target.tenant_id,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM backup_target target
JOIN information_schema.columns source_column
  ON source_column.table_schema = DATABASE()
 AND source_column.table_name = target.table_name
WHERE target.tenant_id = 'default'
  AND target.target_code IN (
      'BACKUP_CUSTOMERS',
      'BACKUP_CUSTOMER_TRANSACTIONS',
      'BACKUP_EMPLOYEES',
      'BACKUP_DAILY_REPORTS'
  )
  AND target.deleted_at IS NULL
ORDER BY target.id, source_column.ordinal_position;
