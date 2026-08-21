-- ProjectAdmin V1 従業員入寮情報
-- MySQL 8.x / 既存データは「入寮なし」で移行する。

SET @dormitory_flag_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'employee'
      AND column_name = 'dormitory_flag'
);
SET @dormitory_flag_sql := IF(
    @dormitory_flag_exists = 0,
    'ALTER TABLE employee ADD COLUMN dormitory_flag BOOLEAN NOT NULL DEFAULT FALSE AFTER address',
    'SELECT 1'
);
PREPARE dormitory_flag_statement FROM @dormitory_flag_sql;
EXECUTE dormitory_flag_statement;
DEALLOCATE PREPARE dormitory_flag_statement;

SET @dormitory_type_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'employee'
      AND column_name = 'dormitory_type'
);
SET @dormitory_type_sql := IF(
    @dormitory_type_exists = 0,
    'ALTER TABLE employee ADD COLUMN dormitory_type VARCHAR(30) NULL AFTER dormitory_flag',
    'SELECT 1'
);
PREPARE dormitory_type_statement FROM @dormitory_type_sql;
EXECUTE dormitory_type_statement;
DEALLOCATE PREPARE dormitory_type_statement;

UPDATE employee
SET dormitory_type = NULL,
    updated_at = NOW(6)
WHERE dormitory_flag = FALSE
  AND dormitory_type IS NOT NULL;

SET @dormitory_check_exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'employee'
      AND constraint_name = 'chk_employee_dormitory'
);
SET @dormitory_check_sql := IF(
    @dormitory_check_exists = 0,
    'ALTER TABLE employee ADD CONSTRAINT chk_employee_dormitory CHECK ((dormitory_flag = FALSE AND dormitory_type IS NULL) OR (dormitory_flag = TRUE AND dormitory_type IN (''SINGLE_ROOM'', ''SHARED_ROOM'')))',
    'SELECT 1'
);
PREPARE dormitory_check_statement FROM @dormitory_check_sql;
EXECUTE dormitory_check_statement;
DEALLOCATE PREPARE dormitory_check_statement;

SET @dormitory_charge_days_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'dormitory_charge_days'
);
SET @dormitory_charge_days_sql := IF(
    @dormitory_charge_days_exists = 0,
    'ALTER TABLE daily_report ADD COLUMN dormitory_charge_days INT NOT NULL DEFAULT 0 AFTER saving_amount',
    'SELECT 1'
);
PREPARE dormitory_charge_days_statement FROM @dormitory_charge_days_sql;
EXECUTE dormitory_charge_days_statement;
DEALLOCATE PREPARE dormitory_charge_days_statement;

SET @dormitory_charge_days_check_exists := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'daily_report'
      AND constraint_name = 'chk_daily_report_dormitory_charge_days'
);
SET @dormitory_charge_days_check_sql := IF(
    @dormitory_charge_days_check_exists = 0,
    'ALTER TABLE daily_report ADD CONSTRAINT chk_daily_report_dormitory_charge_days CHECK (dormitory_charge_days BETWEEN 0 AND 31)',
    'SELECT 1'
);
PREPARE dormitory_charge_days_check_statement FROM @dormitory_charge_days_check_sql;
EXECUTE dormitory_charge_days_check_statement;
DEALLOCATE PREPARE dormitory_charge_days_check_statement;

CREATE TABLE IF NOT EXISTS dormitory_fee_setting (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dormitory_type VARCHAR(30) NOT NULL,
    daily_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    active_flag BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dormitory_fee_setting_type
        UNIQUE (tenant_id, dormitory_type),
    CONSTRAINT chk_dormitory_fee_setting_type
        CHECK (dormitory_type IN ('SINGLE_ROOM', 'SHARED_ROOM')),
    CONSTRAINT chk_dormitory_fee_setting_amount
        CHECK (daily_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 仮設定は誤徴収を防ぐため0円。業務設定画面で正式金額へ変更する。
INSERT INTO dormitory_fee_setting (
    dormitory_type, daily_amount, active_flag,
    tenant_id, created_at, updated_at
)
SELECT room_type.dormitory_type, 0, TRUE,
       tenants.tenant_id, NOW(6), NOW(6)
FROM (
    SELECT 'SINGLE_ROOM' AS dormitory_type
    UNION ALL
    SELECT 'SHARED_ROOM'
) room_type
CROSS JOIN (
    SELECT 'default' AS tenant_id
    UNION
    SELECT DISTINCT tenant_id FROM employee
) tenants
WHERE TRUE
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

-- 寮費Ruleは金額をコードへ固定せず、寮費マスター日額と日報の徴収日数から算出する。
-- 既存Ruleがある場合は管理画面での編集内容を上書きしない。
INSERT INTO rule_master (
    rule_name,
    rule_display_name,
    rule_type,
    dsl_type,
    dsl_text,
    rule_bean_name,
    result_fact_key,
    description,
    priority,
    active_flag,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'DORMITORY_DAILY_FEE',
    '日次寮費',
    'DEDUCTION',
    'JEXL',
    'dormitoryDailyAmount * itemQuantity',
    NULL,
    'result',
    '寮費日額×日報の寮費徴収日数。日報を作成しない休日分は別の日報へまとめられる。',
    100,
    TRUE,
    tenants.tenant_id,
    NOW(6),
    NOW(6),
    NULL
FROM (
    SELECT 'default' AS tenant_id
    UNION
    SELECT DISTINCT tenant_id FROM employee
) tenants
WHERE TRUE
ON DUPLICATE KEY UPDATE rule_name = VALUES(rule_name);

-- 数量は日報明細の汎用quantityからRuleへ渡す。
UPDATE rule_master
SET dsl_text = 'dormitoryDailyAmount * itemQuantity',
    description = '寮費日額×日報明細の消化数量。日報を作成しない休日分は別の日報へまとめられる。',
    updated_at = NOW(6)
WHERE rule_name = 'DORMITORY_DAILY_FEE'
  AND deleted_at IS NULL;

UPDATE rule_parameter parameter_item
JOIN rule_master rule ON rule.id = parameter_item.rule_id
SET parameter_item.deleted_at = NOW(6),
    parameter_item.updated_at = NOW(6)
WHERE rule.rule_name = 'DORMITORY_DAILY_FEE'
  AND rule.deleted_at IS NULL
  AND parameter_item.param_name IN ('dormitoryFlag', 'dormitoryChargeDays')
  AND parameter_item.deleted_at IS NULL;

-- 過去に接続文字コード未指定で投入された表示名だけを補正する。
-- 正常な表示名や管理画面で変更した名称は上書きしない。
UPDATE rule_master
SET rule_display_name = '日次寮費',
    updated_at = NOW(6)
WHERE rule_name = 'DORMITORY_DAILY_FEE'
  AND rule_display_name = 'æ—¥æ¬¡å¯®è²»'
  AND deleted_at IS NULL;

UPDATE rule_master
SET description = '寮費日額×日報の寮費徴収日数。日報を作成しない休日分は別の日報へまとめられる。',
    updated_at = NOW(6)
WHERE rule_name = 'DORMITORY_DAILY_FEE'
  AND HEX(description) LIKE 'C3A5C2AFC2AE%'
  AND deleted_at IS NULL;

INSERT INTO rule_parameter (
    rule_id,
    param_name,
    data_type,
    required_flag,
    default_value,
    description,
    order_no,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    rule.id,
    parameter.param_name,
    parameter.data_type,
    TRUE,
    parameter.default_value,
    parameter.description,
    parameter.order_no,
    rule.tenant_id,
    NOW(6),
    NOW(6),
    NULL
FROM rule_master rule
CROSS JOIN (
    SELECT 'dormitoryDailyAmount' AS param_name,
           'DECIMAL' AS data_type,
           '0' AS default_value,
           '寮費マスターの日額' AS description,
           2 AS order_no
    UNION ALL
    SELECT 'itemQuantity', 'DECIMAL', '0', 'この日報で消化する数量', 3
) parameter
WHERE rule.rule_name = 'DORMITORY_DAILY_FEE'
  AND rule.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM rule_parameter existing
      WHERE existing.rule_id = rule.id
        AND existing.param_name = parameter.param_name
        AND existing.deleted_at IS NULL
  );

UPDATE deduction_masters
SET calculation_type = 'AUTO',
    rule_name = 'DORMITORY_DAILY_FEE',
    default_amount = 0,
    allow_manual_input = FALSE,
    deduction_unit = 'BOTH',
    show_on_daily_statement = TRUE,
    show_on_monthly_statement = TRUE,
    updated_at = NOW(6)
WHERE deduction_code = 'DORMITORY_FEE'
  AND deleted_at IS NULL;
