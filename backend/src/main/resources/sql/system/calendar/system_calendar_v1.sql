-- システム共通カレンダー基盤。
-- 官公庁・金融機関・会社の休日を同じ構造で保持し、利用側が必要な
-- カレンダーと前倒し／後ろ倒し規則を組み合わせる。

CREATE TABLE IF NOT EXISTS system_calendar_holiday (
    id BIGINT NOT NULL AUTO_INCREMENT,
    calendar_type VARCHAR(40) NOT NULL,
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(100) NOT NULL,
    day_type VARCHAR(20) NOT NULL DEFAULT 'CLOSED',
    note VARCHAR(500) NULL,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_system_calendar_holiday
        UNIQUE (tenant_id, calendar_type, holiday_date),
    CONSTRAINT chk_system_calendar_type
        CHECK (calendar_type IN (
            'NATIONAL_HOLIDAY', 'GOVERNMENT',
            'FINANCIAL_INSTITUTION', 'COMPANY'
        )),
    INDEX idx_system_calendar_lookup
        (tenant_id, calendar_type, holiday_date, active_flag, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @calendar_day_type_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'system_calendar_holiday'
      AND column_name = 'day_type'
);
SET @calendar_day_type_ddl = IF(
    @calendar_day_type_exists = 0,
    'ALTER TABLE system_calendar_holiday ADD COLUMN day_type VARCHAR(20) NOT NULL DEFAULT ''CLOSED'' AFTER holiday_name',
    'SELECT 1'
);
PREPARE stmt FROM @calendar_day_type_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 既存環境のカレンダー種別制約へ国民の祝日を追加する。
SET @calendar_type_check_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'system_calendar_holiday'
      AND constraint_name = 'chk_system_calendar_type'
      AND constraint_type = 'CHECK'
);
SET @calendar_type_check_drop = IF(
    @calendar_type_check_exists > 0,
    'ALTER TABLE system_calendar_holiday DROP CHECK chk_system_calendar_type',
    'SELECT 1'
);
PREPARE stmt FROM @calendar_type_check_drop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE system_calendar_holiday
    ADD CONSTRAINT chk_system_calendar_type
    CHECK (calendar_type IN (
        'NATIONAL_HOLIDAY', 'GOVERNMENT',
        'FINANCIAL_INSTITUTION', 'COMPANY'
    ));

CREATE TABLE IF NOT EXISTS system_calendar_weekday_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    calendar_type VARCHAR(40) NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    closed_flag BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_system_calendar_weekday_rule
        UNIQUE (tenant_id, calendar_type, day_of_week),
    INDEX idx_system_calendar_weekday_lookup
        (tenant_id, calendar_type, day_of_week, closed_flag, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_calendar_reference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    calendar_type VARCHAR(40) NOT NULL,
    source_calendar_type VARCHAR(40) NOT NULL,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_system_calendar_reference
        UNIQUE (tenant_id, calendar_type, source_calendar_type),
    INDEX idx_system_calendar_reference_lookup
        (tenant_id, calendar_type, source_calendar_type, active_flag, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V1の会社休日を新しい独立ドメインへ移行する。
INSERT INTO system_calendar_holiday (
    calendar_type,
    holiday_date,
    holiday_name,
    day_type,
    note,
    active_flag,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'COMPANY',
    holiday.holiday_date,
    holiday.holiday_name,
    'CLOSED',
    holiday.note,
    holiday.active_flag,
    holiday.tenant_id,
    holiday.created_at,
    holiday.updated_at,
    holiday.deleted_at
FROM business_holiday holiday
ON DUPLICATE KEY UPDATE
    holiday_name = VALUES(holiday_name),
    note = VALUES(note),
    active_flag = VALUES(active_flag),
    updated_at = VALUES(updated_at),
    deleted_at = VALUES(deleted_at);

-- 既存テナントには、従来仕様のお盆（8/13〜15）を会社休日として引き継ぐ。
INSERT INTO system_calendar_holiday (
    calendar_type,
    holiday_date,
    holiday_name,
    day_type,
    note,
    active_flag,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'COMPANY',
    calendar.calendar_date,
    'お盆休み',
    'CLOSED',
    NULL,
    TRUE,
    tenant.tenant_id,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL
FROM business_calendar_date calendar
CROSS JOIN (
    SELECT DISTINCT profile.tenant_id
    FROM company_profile profile
    WHERE profile.deleted_at IS NULL
) tenant
WHERE MONTH(calendar.calendar_date) = 8
  AND DAY(calendar.calendar_date) BETWEEN 13 AND 15
ON DUPLICATE KEY UPDATE
    holiday_name = holiday_name;

-- 現行運用は土曜日を通常日として扱い、日曜日だけを会社定休日とする。
INSERT INTO system_calendar_weekday_rule (
    calendar_type, day_of_week, closed_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    'COMPANY', 'SUNDAY', TRUE,
    tenant.tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM (
    SELECT DISTINCT tenant_id FROM company_profile WHERE deleted_at IS NULL
    UNION
    SELECT DISTINCT tenant_id FROM system_calendar_holiday WHERE deleted_at IS NULL
) tenant
WHERE NOT EXISTS (
    SELECT 1
    FROM system_calendar_weekday_rule existing
    WHERE existing.tenant_id = tenant.tenant_id
      AND existing.calendar_type = 'COMPANY'
      AND existing.deleted_at IS NULL
);

-- 先行版で官公庁休日として取り込まれた公式祝日を共通カレンダーへ移す。
INSERT INTO system_calendar_holiday (
    calendar_type, holiday_date, holiday_name, day_type, note,
    active_flag, tenant_id, created_at, updated_at, deleted_at
)
SELECT
    'NATIONAL_HOLIDAY', holiday_date, holiday_name, day_type, note,
    active_flag, tenant_id, created_at, updated_at, deleted_at
FROM system_calendar_holiday
WHERE calendar_type = 'GOVERNMENT'
  AND note = '内閣府公式CSV'
ON DUPLICATE KEY UPDATE
    holiday_name = VALUES(holiday_name),
    day_type = VALUES(day_type),
    note = VALUES(note),
    active_flag = VALUES(active_flag),
    updated_at = VALUES(updated_at),
    deleted_at = VALUES(deleted_at);

DELETE FROM system_calendar_holiday
WHERE calendar_type = 'GOVERNMENT'
  AND note = '内閣府公式CSV';

-- 内閣府の公式CSVを既存の外部データ取込基盤から取り込む定義。
INSERT INTO import_target (
    target_code, target_name, table_name, description,
    source_type, fixed_file_path, script_type, script_path, script_args,
    import_mode, header_row_number, data_start_row_number,
    charset, delimiter, active_flag,
    tenant_id, created_at, updated_at, deleted_at
) VALUES (
    'IMPORT_NATIONAL_HOLIDAY', '国民の祝日取込', 'system_calendar_holiday',
    '内閣府の公式CSVをアップロードし、国民の祝日カレンダーへ取り込む',
    'UPLOAD', 'national_holidays.csv', 'PYTHON',
    'convert_national_holidays_v2.py',
    '--input ${IMPORT_INPUT_FILE} --output ${IMPORT_CSV_DIR}/national_holidays.csv',
    'UPSERT', 1, 2, 'UTF-8', ',', TRUE,
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
)
ON DUPLICATE KEY UPDATE
    target_name = VALUES(target_name),
    table_name = VALUES(table_name),
    description = VALUES(description),
    source_type = VALUES(source_type),
    fixed_file_path = VALUES(fixed_file_path),
    script_type = VALUES(script_type),
    script_path = VALUES(script_path),
    script_args = VALUES(script_args),
    import_mode = VALUES(import_mode),
    header_row_number = VALUES(header_row_number),
    data_start_row_number = VALUES(data_start_row_number),
    charset = VALUES(charset),
    delimiter = VALUES(delimiter),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL;

INSERT INTO import_target_catalog (
    table_name, display_name, description,
    tenant_scoped_flag, allow_delete_insert_flag, active_flag,
    tenant_id, created_at, updated_at, deleted_at
) VALUES (
    'system_calendar_holiday', 'システムカレンダー休日',
    '国民の祝日・官公庁・金融機関・会社休日の日付データ',
    TRUE, FALSE, TRUE,
    'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    tenant_scoped_flag = VALUES(tenant_scoped_flag),
    allow_delete_insert_flag = VALUES(allow_delete_insert_flag),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6),
    deleted_at = NULL;

CREATE TEMPORARY TABLE tmp_calendar_import_column (
    column_name VARCHAR(200) NOT NULL,
    csv_header_name VARCHAR(200) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    required_flag TINYINT(1) NOT NULL,
    key_flag TINYINT(1) NOT NULL,
    nullable_flag TINYINT(1) NOT NULL,
    updatable_flag TINYINT(1) NOT NULL,
    format_pattern VARCHAR(100) NULL,
    order_no INT NOT NULL,
    PRIMARY KEY (column_name)
);

INSERT INTO tmp_calendar_import_column VALUES
('calendar_type', 'calendarType', 'STRING', 1, 1, 0, 0, NULL, 1),
('holiday_date', 'holidayDate', 'DATE', 1, 1, 0, 0, 'yyyy-MM-dd', 2),
('holiday_name', 'holidayName', 'STRING', 1, 0, 0, 1, NULL, 3),
('day_type', 'dayType', 'STRING', 1, 0, 0, 1, NULL, 4),
('note', 'note', 'STRING', 0, 0, 1, 1, NULL, 5),
('active_flag', 'activeFlag', 'BOOLEAN', 1, 0, 0, 1, NULL, 6);

UPDATE import_column column_def
JOIN import_target target ON target.id = column_def.target_id
JOIN tmp_calendar_import_column expected
  ON expected.column_name = column_def.column_name
SET
    column_def.csv_header_name = expected.csv_header_name,
    column_def.data_type = expected.data_type,
    column_def.required_flag = expected.required_flag,
    column_def.key_flag = expected.key_flag,
    column_def.nullable_flag = expected.nullable_flag,
    column_def.trim_flag = TRUE,
    column_def.format_pattern = expected.format_pattern,
    column_def.updatable_flag = expected.updatable_flag,
    column_def.order_no = expected.order_no,
    column_def.updated_at = CURRENT_TIMESTAMP(6),
    column_def.deleted_at = NULL
WHERE target.target_code = 'IMPORT_NATIONAL_HOLIDAY';

INSERT INTO import_column (
    target_id, column_name, csv_header_name, data_type,
    required_flag, key_flag, nullable_flag, trim_flag,
    default_value, format_pattern, updatable_flag, order_no,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    target.id, expected.column_name, expected.csv_header_name, expected.data_type,
    expected.required_flag, expected.key_flag, expected.nullable_flag, TRUE,
    NULL, expected.format_pattern, expected.updatable_flag, expected.order_no,
    target.tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tmp_calendar_import_column expected
JOIN import_target target ON target.target_code = 'IMPORT_NATIONAL_HOLIDAY'
WHERE NOT EXISTS (
    SELECT 1 FROM import_column existing
    WHERE existing.target_id = target.id
      AND existing.column_name = expected.column_name
      AND existing.deleted_at IS NULL
);

INSERT INTO import_target_catalog_column (
    catalog_id, column_name, display_name, data_type,
    order_no, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    catalog.id, expected.column_name, expected.csv_header_name,
    expected.data_type, expected.order_no, TRUE,
    catalog.tenant_id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
FROM tmp_calendar_import_column expected
JOIN import_target_catalog catalog
  ON catalog.table_name = 'system_calendar_holiday'
 AND catalog.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM import_target_catalog_column existing
    WHERE existing.catalog_id = catalog.id
      AND existing.column_name = expected.column_name
      AND existing.deleted_at IS NULL
);

DROP TEMPORARY TABLE tmp_calendar_import_column;

