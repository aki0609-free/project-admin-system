-- ProjectAdmin V1 会社休日・年末調整
-- 日曜日とお盆（8/13〜15）だけを休日とする。土曜日・国民の祝日は通常日。

CREATE TABLE IF NOT EXISTS business_holiday (
    id BIGINT NOT NULL AUTO_INCREMENT,
    holiday_date DATE NOT NULL,
    holiday_name VARCHAR(100) NOT NULL,
    holiday_type VARCHAR(30) NOT NULL DEFAULT 'OBON',
    note VARCHAR(500) NULL,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_business_holiday_date
        UNIQUE (tenant_id, holiday_date),
    CONSTRAINT chk_business_holiday_type
        CHECK (holiday_type IN ('OBON')),
    INDEX idx_business_holiday_calendar
        (tenant_id, holiday_date, active_flag, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 正の金額は還付、負の金額は追加徴収として扱う。
CREATE TABLE IF NOT EXISTS employee_year_end_adjustment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    tax_year INT NOT NULL,
    target_month DATE NOT NULL,
    adjustment_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    note VARCHAR(500) NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_year_end_adjustment
        UNIQUE (tenant_id, employee_id, tax_year),
    CONSTRAINT fk_year_end_adjustment_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT chk_year_end_adjustment_status
        CHECK (status IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT chk_year_end_adjustment_tax_year
        CHECK (tax_year BETWEEN 2000 AND 2200),
    INDEX idx_year_end_adjustment_target
        (tenant_id, target_month, status, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- SQL Viewからも同じ営業日補正を利用できるよう、2000〜2200年の
-- 日付ディメンションをテナント非依存で保持する。
CREATE TABLE IF NOT EXISTS business_calendar_date (
    calendar_date DATE NOT NULL,
    day_of_week TINYINT NOT NULL,
    weekend_flag BOOLEAN NOT NULL,
    PRIMARY KEY (calendar_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO business_calendar_date (calendar_date, day_of_week, weekend_flag)
SELECT generated_dates.calendar_date,
       DAYOFWEEK(generated_dates.calendar_date),
       DAYOFWEEK(generated_dates.calendar_date) = 1
FROM (
    SELECT DATE_ADD('2000-01-01', INTERVAL numbers.number_value DAY) AS calendar_date
    FROM (
        SELECT ones.n + tens.n * 10 + hundreds.n * 100
             + thousands.n * 1000 + ten_thousands.n * 10000 AS number_value
        FROM
            (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
             UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
        CROSS JOIN
            (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
             UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
        CROSS JOIN
            (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
             UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
        CROSS JOIN
            (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
             UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) thousands
        CROSS JOIN
            (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
             UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ten_thousands
    ) numbers
    WHERE numbers.number_value <= DATEDIFF('2200-12-31', '2000-01-01')
) generated_dates
ON DUPLICATE KEY UPDATE
    day_of_week = VALUES(day_of_week),
    weekend_flag = VALUES(weekend_flag);

CREATE OR REPLACE VIEW vw_year_end_adjustment_confirmed AS
SELECT
    adjustment.tenant_id,
    adjustment.target_month,
    adjustment.employee_id,
    adjustment.tax_year,
    adjustment.adjustment_amount
FROM employee_year_end_adjustment adjustment
WHERE adjustment.status = 'CONFIRMED'
  AND adjustment.deleted_at IS NULL;
