-- ProjectAdminSystem V1
-- 明細到着型・月次運用型の手当控除を保存する共通取引基盤。
-- 項目ごとの専用テーブルは作らず、控除マスターとtarget_codeで関連付ける。

SET NAMES utf8mb4;

SET @balance_tracking_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payroll_item_balance_policy'
      AND column_name = 'balance_tracking_flag'
);
SET @balance_tracking_sql := IF(
    @balance_tracking_exists = 0,
    'ALTER TABLE payroll_item_balance_policy ADD COLUMN balance_tracking_flag BOOLEAN NOT NULL DEFAULT TRUE AFTER balance_unit',
    'SELECT 1'
);
PREPARE statement FROM @balance_tracking_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @input_source_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'payroll_item_balance_policy'
      AND column_name = 'input_source'
);
SET @input_source_sql := IF(
    @input_source_exists = 0,
    'ALTER TABLE payroll_item_balance_policy ADD COLUMN input_source VARCHAR(30) NOT NULL DEFAULT ''DAILY_REPORT'' AFTER balance_tracking_flag',
    'SELECT 1'
);
PREPARE statement FROM @input_source_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

UPDATE payroll_item_balance_policy
SET balance_tracking_flag = TRUE,
    input_source = 'DAILY_REPORT',
    updated_at = NOW(6)
WHERE target_type = 'DEDUCTION'
  AND target_code = 'DORMITORY_FEE'
  AND deleted_at IS NULL;

UPDATE payroll_item_balance_policy
SET balance_tracking_flag = FALSE,
    input_source = 'TRANSACTION',
    updated_at = NOW(6)
WHERE target_type = 'DEDUCTION'
  AND target_code = 'MOBILE_RENTAL'
  AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS employee_payroll_item_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_master_id BIGINT NOT NULL,
    target_code VARCHAR(50) NOT NULL,
    target_name VARCHAR(200) NOT NULL,
    target_month DATE NOT NULL,
    transaction_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    quantity DECIMAL(12,2) NULL,
    balance_effect VARCHAR(20) NOT NULL DEFAULT 'NONE',
    source_type VARCHAR(30) NOT NULL,
    source_reference VARCHAR(150) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    note VARCHAR(500) NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_payroll_item_transaction_month (
        tenant_id, target_month, employee_id, status, deleted_at
    ),
    INDEX idx_payroll_item_transaction_target (
        tenant_id, target_type, target_code, transaction_date
    ),
    CONSTRAINT uk_employee_payroll_item_transaction_source UNIQUE (
        tenant_id, employee_id, target_type, target_code, source_reference
    ),
    CONSTRAINT fk_payroll_item_transaction_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT chk_payroll_item_transaction_target_type
        CHECK (target_type IN ('ALLOWANCE', 'DEDUCTION')),
    CONSTRAINT chk_payroll_item_transaction_source_type
        CHECK (source_type IN ('MANUAL', 'CSV', 'EXTERNAL', 'MONTHLY_OPERATION')),
    CONSTRAINT chk_payroll_item_transaction_balance_effect
        CHECK (balance_effect IN ('NONE', 'CREDIT', 'DEBIT')),
    CONSTRAINT chk_payroll_item_transaction_status
        CHECK (status IN ('DRAFT', 'CONFIRMED')),
    CONSTRAINT chk_payroll_item_transaction_amount
        CHECK (amount > 0),
    CONSTRAINT chk_payroll_item_transaction_month
        CHECK (DAY(target_month) = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @transaction_balance_effect_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'employee_payroll_item_transaction'
      AND column_name = 'balance_effect'
);
SET @transaction_balance_effect_sql := IF(
    @transaction_balance_effect_exists = 0,
    'ALTER TABLE employee_payroll_item_transaction ADD COLUMN balance_effect VARCHAR(20) NOT NULL DEFAULT ''NONE'' AFTER quantity',
    'SELECT 1'
);
PREPARE statement FROM @transaction_balance_effect_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

CREATE OR REPLACE VIEW vw_employee_payroll_item_transaction_confirmed AS
SELECT
    transaction_item.tenant_id,
    transaction_item.target_month,
    transaction_item.employee_id,
    CASE
        WHEN transaction_item.target_type = 'ALLOWANCE' THEN 'ALLOWANCE'
        WHEN deduction.deduction_type = 'LEGAL' THEN 'LEGAL_DEDUCTION'
        ELSE 'OTHER_DEDUCTION'
    END AS item_category,
    transaction_item.target_code AS item_code,
    MAX(transaction_item.target_name) AS item_name,
    COALESCE(allowance.display_order, deduction.display_order, 9000) AS display_order,
    SUM(transaction_item.amount) AS item_value
FROM employee_payroll_item_transaction transaction_item
LEFT JOIN deduction_masters deduction
  ON deduction.tenant_id = transaction_item.tenant_id
 AND deduction.id = transaction_item.target_master_id
 AND transaction_item.target_type = 'DEDUCTION'
 AND deduction.deleted_at IS NULL
LEFT JOIN allowance_masters allowance
  ON allowance.tenant_id = transaction_item.tenant_id
 AND allowance.id = transaction_item.target_master_id
 AND transaction_item.target_type = 'ALLOWANCE'
 AND allowance.deleted_at IS NULL
WHERE transaction_item.status = 'CONFIRMED'
  AND transaction_item.deleted_at IS NULL
GROUP BY
    transaction_item.tenant_id,
    transaction_item.target_month,
    transaction_item.employee_id,
    CASE
        WHEN transaction_item.target_type = 'ALLOWANCE' THEN 'ALLOWANCE'
        WHEN deduction.deduction_type = 'LEGAL' THEN 'LEGAL_DEDUCTION'
        ELSE 'OTHER_DEDUCTION'
    END,
    transaction_item.target_code,
    allowance.display_order,
    deduction.display_order;

-- 日報明細と明細取引を同じイベント形式で参照する残高台帳View。
CREATE OR REPLACE VIEW vw_employee_payroll_item_balance_event AS
SELECT report.tenant_id,
       report.employee_id,
       'ALLOWANCE' AS target_type,
       item.allowance_master_id AS target_master_id,
       item.allowance_code AS target_code,
       report.work_date AS event_date,
       'DAILY_REPORT' AS source_type,
       CAST(report.id AS CHAR) AS source_reference,
       'DEBIT' AS balance_effect,
       COALESCE(item.quantity, 0) AS quantity
FROM daily_report_allowances item
JOIN daily_report report ON report.id = item.daily_report_id
WHERE report.deleted_at IS NULL
  AND item.quantity IS NOT NULL
UNION ALL
SELECT report.tenant_id,
       report.employee_id,
       'DEDUCTION' AS target_type,
       item.deduction_master_id AS target_master_id,
       item.deduction_code AS target_code,
       report.work_date AS event_date,
       'DAILY_REPORT' AS source_type,
       CAST(report.id AS CHAR) AS source_reference,
       'DEBIT' AS balance_effect,
       COALESCE(item.quantity, 0) AS quantity
FROM daily_report_deductions item
JOIN daily_report report ON report.id = item.daily_report_id
WHERE report.deleted_at IS NULL
  AND item.quantity IS NOT NULL
UNION ALL
SELECT transaction_item.tenant_id,
       transaction_item.employee_id,
       transaction_item.target_type,
       transaction_item.target_master_id,
       transaction_item.target_code,
       transaction_item.transaction_date AS event_date,
       transaction_item.source_type,
       COALESCE(transaction_item.source_reference,
                CAST(transaction_item.id AS CHAR)) AS source_reference,
       transaction_item.balance_effect,
       COALESCE(transaction_item.quantity, 0) AS quantity
FROM employee_payroll_item_transaction transaction_item
WHERE transaction_item.status = 'CONFIRMED'
  AND transaction_item.deleted_at IS NULL
  AND transaction_item.balance_effect <> 'NONE';
