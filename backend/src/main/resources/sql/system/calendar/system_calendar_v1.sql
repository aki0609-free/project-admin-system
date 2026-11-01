-- システム共通カレンダー基盤。
-- 官公庁・金融機関・会社の休日を同じ構造で保持し、利用側が必要な
-- カレンダーと前倒し／後ろ倒し規則を組み合わせる。

CREATE TABLE IF NOT EXISTS system_calendar_holiday (
    id BIGINT NOT NULL AUTO_INCREMENT,
    calendar_type VARCHAR(40) NOT NULL,
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(100) NOT NULL,
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
            'GOVERNMENT', 'FINANCIAL_INSTITUTION', 'COMPANY'
        )),
    INDEX idx_system_calendar_lookup
        (tenant_id, calendar_type, holiday_date, active_flag, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V1の会社休日を新しい独立ドメインへ移行する。
INSERT INTO system_calendar_holiday (
    calendar_type,
    holiday_date,
    holiday_name,
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

