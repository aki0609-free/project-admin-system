-- 会社（テナント）・適用期間ごとの給与計算制度
-- 日報の勤務日を基準に有効な1件を選択し、値をRuleへ渡す。

CREATE TABLE IF NOT EXISTS payroll_policy_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    week_start_day VARCHAR(20) NOT NULL,
    weekly_statutory_hours DECIMAL(8, 2) NOT NULL,
    monthly_overtime_threshold_hours DECIMAL(8, 2) NOT NULL,
    overtime_rate DECIMAL(6, 3) NOT NULL,
    overtime_over_threshold_rate DECIMAL(6, 3) NOT NULL,
    night_premium_rate DECIMAL(6, 3) NOT NULL,
    statutory_holiday_rate DECIMAL(6, 3) NOT NULL,
    daily_standard_hours DECIMAL(6, 2) NOT NULL,
    amount_rounding_mode VARCHAR(20) NOT NULL,
    active_flag BOOLEAN NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_payroll_policy_effective (
        tenant_id, active_flag, effective_from, effective_to, deleted_at
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 現行Java固定値と同じ初期設定を、未設定テナントだけに配置する。
INSERT INTO payroll_policy_setting (
    effective_from,
    effective_to,
    week_start_day,
    weekly_statutory_hours,
    monthly_overtime_threshold_hours,
    overtime_rate,
    overtime_over_threshold_rate,
    night_premium_rate,
    statutory_holiday_rate,
    daily_standard_hours,
    amount_rounding_mode,
    active_flag,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    '2000-01-01',
    NULL,
    'MONDAY',
    40.00,
    60.00,
    1.250,
    1.500,
    0.250,
    1.350,
    8.00,
    'HALF_UP',
    TRUE,
    tenants.tenant_id,
    NOW(6),
    NOW(6),
    NULL
FROM (
    SELECT 'default' AS tenant_id
    UNION
    SELECT DISTINCT tenant_id FROM users WHERE deleted_at IS NULL
    UNION
    SELECT DISTINCT tenant_id FROM employee WHERE deleted_at IS NULL
) tenants
WHERE NOT EXISTS (
    SELECT 1
    FROM payroll_policy_setting setting_row
    WHERE setting_row.tenant_id = tenants.tenant_id
      AND setting_row.deleted_at IS NULL
);
