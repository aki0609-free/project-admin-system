-- 車両手配区分、顧客別距離請求単価、社員手配時の運転手当のV1基盤。

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE customers ADD COLUMN distance_billing_unit_price DECIMAL(10,2) NOT NULL DEFAULT 30.00 AFTER job_type',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'customers'
      AND column_name = 'distance_billing_unit_price'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Hibernateが先に列を作成した環境でも、既存INSERTとの互換性を保証する。
ALTER TABLE customers
    MODIFY COLUMN distance_billing_unit_price DECIMAL(10,2) NOT NULL DEFAULT 30.00;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN vehicle_arrangement_type VARCHAR(20) NOT NULL DEFAULT ''NONE'' AFTER vehicle_used_flag',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'vehicle_arrangement_type'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE daily_report
    MODIFY COLUMN vehicle_arrangement_type VARCHAR(20) NOT NULL DEFAULT 'NONE';

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_report ADD COLUMN passenger_count INT NOT NULL DEFAULT 0 AFTER mileage',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_report'
      AND column_name = 'passenger_count'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE daily_report
    MODIFY COLUMN passenger_count INT NOT NULL DEFAULT 0;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_preparation_assignments ADD COLUMN vehicle_arrangement_type VARCHAR(20) NOT NULL DEFAULT ''NONE'' AFTER site_name',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_preparation_assignments'
      AND column_name = 'vehicle_arrangement_type'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE daily_preparation_assignments
    MODIFY COLUMN vehicle_arrangement_type VARCHAR(20) NOT NULL DEFAULT 'NONE';

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_preparation_assignments ADD COLUMN passenger_count INT NOT NULL DEFAULT 0 AFTER vehicle_arrangement_type',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_preparation_assignments'
      AND column_name = 'passenger_count'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE daily_preparation_assignments
    MODIFY COLUMN passenger_count INT NOT NULL DEFAULT 0;

-- 現場マスターの距離は初期値とし、勤務日ごとの実距離・調整額を配車へ保持する。
SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE daily_preparation_dispatches ADD COLUMN other_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER vehicle_count',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'daily_preparation_dispatches'
      AND column_name = 'other_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE daily_preparation_dispatches
    MODIFY COLUMN other_amount DECIMAL(15,2) NOT NULL DEFAULT 0;

-- 旧「車両使用」は従業員が運転した入力として互換変換する。
UPDATE daily_report
SET vehicle_arrangement_type = 'EMPLOYEE'
WHERE vehicle_used_flag = TRUE
  AND vehicle_arrangement_type = 'NONE';

-- 運転手当は金額をJavaへ固定せず、Ruleパラメータで変更可能にする。
INSERT INTO rule_master (
    rule_name, rule_display_name, rule_type, dsl_type, dsl_text,
    rule_bean_name, result_fact_key, description, priority, active_flag,
    tenant_id, created_at, updated_at, deleted_at
)
SELECT
    'DAILY_DRIVER_ALLOWANCE',
    '日次運転手当',
    'ALLOWANCE',
    'JEXL',
    'vehicleArrangementType == ''EMPLOYEE'' ? mileage * distanceUnitPrice + passengerCount * passengerUnitPrice : 0',
    NULL,
    'result',
    '社員手配時の走行距離と、運転手本人を除く同乗者数から運転手当を計算する。',
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
ON DUPLICATE KEY UPDATE
    rule_display_name = VALUES(rule_display_name),
    dsl_text = VALUES(dsl_text),
    description = VALUES(description),
    active_flag = TRUE,
    deleted_at = NULL,
    updated_at = VALUES(updated_at);

INSERT INTO rule_parameter (
    rule_id, param_name, data_type, required_flag, default_value,
    description, order_no, tenant_id, created_at, updated_at, deleted_at
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
    SELECT 'vehicleArrangementType' AS param_name, 'STRING' AS data_type,
           'NONE' AS default_value, '車両手配区分' AS description, 1 AS order_no
    UNION ALL SELECT 'mileage', 'DECIMAL', '0', '走行距離（km）', 2
    UNION ALL SELECT 'passengerCount', 'INTEGER', '0', '運転手本人を除く同乗者数', 3
    UNION ALL SELECT 'distanceUnitPrice', 'DECIMAL', '15', '1km当たりの運転手当', 4
    UNION ALL SELECT 'passengerUnitPrice', 'DECIMAL', '200', '同乗者1名当たりの運転手当', 5
) parameter
WHERE rule.rule_name = 'DAILY_DRIVER_ALLOWANCE'
  AND rule.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM rule_parameter existing
      WHERE existing.rule_id = rule.id
        AND existing.param_name = parameter.param_name
        AND existing.deleted_at IS NULL
  );

UPDATE allowance_masters
SET calculation_type = 'AUTO',
    rule_name = 'DAILY_DRIVER_ALLOWANCE',
    default_amount = 0,
    allow_manual_input = FALSE,
    note = '社員手配時：走行距離×距離単価＋同乗者数×同乗者単価',
    updated_at = NOW(6)
WHERE allowance_code = 'DRIVER_ALLOWANCE'
  AND deleted_at IS NULL;
