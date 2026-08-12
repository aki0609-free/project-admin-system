-- ProjectAdminSystem V1
-- 日報給与内訳Rule・保存項目の基盤

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN normal_pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER saving_amount',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'normal_pay_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN holiday_premium_eligible TINYINT(1) NOT NULL DEFAULT 0 AFTER holiday_work_hours',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'holiday_premium_eligible'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN overtime_pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER normal_pay_amount',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'overtime_pay_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN night_pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER overtime_pay_amount',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'night_pay_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN holiday_pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER night_pay_amount',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'holiday_pay_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS daily_pay_rule_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    component_type VARCHAR(30) NOT NULL,
    rule_name VARCHAR(150) NOT NULL,
    active_flag TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_daily_pay_rule_setting_tenant_component
        UNIQUE (tenant_id, component_type)
);

-- 寮費は会社への支払いは月次だが、従業員からは日次徴収する。
-- 日報控除明細へ日ごとの確定額を保存し、月次はその合計を使用する。
UPDATE deduction_masters
SET deduction_unit = 'BOTH',
    show_on_daily_statement = 1,
    show_on_monthly_statement = 1,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE deduction_code = 'DORMITORY_FEE'
  AND deleted_at IS NULL;

-- 法定割増の初期Rule。既存Ruleの管理画面編集内容は上書きしない。
INSERT INTO rule_master (
    rule_name, rule_display_name, rule_type, dsl_type, dsl_text,
    rule_bean_name, result_fact_key, description, priority, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT definition.rule_name,
       definition.display_name,
       'PAYROLL',
       'JEXL',
       definition.dsl_text,
       NULL,
       'result',
       definition.description,
       100,
       TRUE,
       tenants.tenant_id,
       CURRENT_TIMESTAMP(6),
       CURRENT_TIMESTAMP(6),
       NULL
FROM (
    SELECT 'DAILY_NORMAL_PAY' AS rule_name,
           '日次通常給与' AS display_name,
           'regularPayAmount' AS dsl_text,
           '給与計算用時間単価×週40時間以内の通常時間' AS description
    UNION ALL
    SELECT 'DAILY_OVERTIME_PAY',
           '日次時間外手当',
           'calculationHourlyRate * (overtimeWithin60Hours * overtimeRate + overtimeOver60Hours * overtimeOver60Rate)',
           '法定時間外25%、月60時間超50%の初期Rule'
    UNION ALL
    SELECT 'DAILY_NIGHT_PAY',
           '日次深夜手当',
           'calculationHourlyRate * nightWorkHours * nightPremiumRate',
           '22時から翌5時までの深夜割増25%の初期Rule'
    UNION ALL
    SELECT 'DAILY_HOLIDAY_PAY',
           '日次休日手当',
           'calculationHourlyRate * holidayWorkHours * holidayRate',
           '休日手当対象として明示された勤務へ35%割増を含む初期Rule'
) definition
CROSS JOIN (
    SELECT 'default' AS tenant_id
    UNION
    SELECT DISTINCT tenant_id FROM employee
) tenants
WHERE NOT EXISTS (
    SELECT 1
    FROM rule_master existing
    WHERE existing.tenant_id = tenants.tenant_id
      AND existing.rule_name = definition.rule_name
      AND existing.deleted_at IS NULL
)
ON DUPLICATE KEY UPDATE
    deleted_at = NULL,
    updated_at = VALUES(updated_at);

INSERT INTO rule_parameter (
    rule_id, param_name, data_type, required_flag, default_value,
    description, order_no, tenant_id,
    created_at, updated_at, deleted_at
)
SELECT rule.id,
       parameter.param_name,
       'DECIMAL',
       TRUE,
       parameter.default_value,
       parameter.description,
       parameter.order_no,
       rule.tenant_id,
       CURRENT_TIMESTAMP(6),
       CURRENT_TIMESTAMP(6),
       NULL
FROM rule_master rule
JOIN (
    SELECT 'DAILY_NORMAL_PAY' AS rule_name,
           'regularPayAmount' AS param_name,
           '0' AS default_value,
           '通常給与額' AS description,
           1 AS order_no
    UNION ALL
    SELECT 'DAILY_OVERTIME_PAY', 'calculationHourlyRate', '0', '給与計算用時間単価', 1
    UNION ALL
    SELECT 'DAILY_OVERTIME_PAY', 'overtimeWithin60Hours', '0', '月60時間以内の法定時間外時間', 2
    UNION ALL
    SELECT 'DAILY_OVERTIME_PAY', 'overtimeOver60Hours', '0', '月60時間を超える法定時間外時間', 3
    UNION ALL
    SELECT 'DAILY_OVERTIME_PAY', 'overtimeRate', '1.25', '法定時間外支給率', 4
    UNION ALL
    SELECT 'DAILY_OVERTIME_PAY', 'overtimeOver60Rate', '1.50', '月60時間超支給率', 5
    UNION ALL
    SELECT 'DAILY_NIGHT_PAY', 'calculationHourlyRate', '0', '給与計算用時間単価', 1
    UNION ALL
    SELECT 'DAILY_NIGHT_PAY', 'nightWorkHours', '0', '深夜労働時間', 2
    UNION ALL
    SELECT 'DAILY_NIGHT_PAY', 'nightPremiumRate', '0.25', '深夜加算率', 3
    UNION ALL
    SELECT 'DAILY_HOLIDAY_PAY', 'calculationHourlyRate', '0', '給与計算用時間単価', 1
    UNION ALL
    SELECT 'DAILY_HOLIDAY_PAY', 'holidayWorkHours', '0', '休日労働時間', 2
    UNION ALL
    SELECT 'DAILY_HOLIDAY_PAY', 'holidayRate', '1.35', '休日支給率', 3
) parameter ON parameter.rule_name = rule.rule_name
WHERE rule.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM rule_parameter existing
      WHERE existing.rule_id = rule.id
        AND existing.param_name = parameter.param_name
        AND existing.deleted_at IS NULL
  );

INSERT INTO daily_pay_rule_setting (
    component_type, rule_name, active_flag,
    created_at, updated_at, tenant_id
)
SELECT mapping.component_type,
       mapping.rule_name,
       TRUE,
       CURRENT_TIMESTAMP(6),
       CURRENT_TIMESTAMP(6),
       tenants.tenant_id
FROM (
    SELECT 'NORMAL_PAY' AS component_type, 'DAILY_NORMAL_PAY' AS rule_name
    UNION ALL
    SELECT 'OVERTIME_PAY', 'DAILY_OVERTIME_PAY'
    UNION ALL
    SELECT 'NIGHT_PAY', 'DAILY_NIGHT_PAY'
    UNION ALL
    SELECT 'HOLIDAY_PAY', 'DAILY_HOLIDAY_PAY'
) mapping
CROSS JOIN (
    SELECT 'default' AS tenant_id
    UNION
    SELECT DISTINCT tenant_id FROM employee
) tenants
WHERE TRUE
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at);
