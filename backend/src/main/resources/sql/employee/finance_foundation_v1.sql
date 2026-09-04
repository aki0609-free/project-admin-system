-- ProjectAdmin V1 従業員貸付・積立基盤
-- MySQL 8.x / 既存データを削除しない補助DDL

-- V1では承認フローを使用せず、有効な設定は承認済みとして扱う。
UPDATE employee_loan
SET approval_status = 'APPROVED',
    approval_comment = NULL,
    updated_at = NOW(6)
WHERE active_flag = TRUE
  AND deleted_at IS NULL
  AND approval_status <> 'APPROVED';

UPDATE employee_saving
SET approval_status = 'APPROVED',
    approval_comment = NULL,
    updated_at = NOW(6)
WHERE active_flag = TRUE
  AND deleted_at IS NULL
  AND approval_status <> 'APPROVED';

SET @loan_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'employee_loan'
      AND index_name = 'idx_employee_loan_active'
);
SET @loan_index_sql := IF(
    @loan_index_exists = 0,
    'CREATE INDEX idx_employee_loan_active ON employee_loan (tenant_id, employee_id, active_flag, deleted_at)',
    'SELECT 1'
);
PREPARE loan_index_statement FROM @loan_index_sql;
EXECUTE loan_index_statement;
DEALLOCATE PREPARE loan_index_statement;

SET @saving_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'employee_saving'
      AND index_name = 'idx_employee_saving_active'
);
SET @saving_index_sql := IF(
    @saving_index_exists = 0,
    'CREATE INDEX idx_employee_saving_active ON employee_saving (tenant_id, employee_id, active_flag, deleted_at)',
    'SELECT 1'
);
PREPARE saving_index_statement FROM @saving_index_sql;
EXECUTE saving_index_statement;
DEALLOCATE PREPARE saving_index_statement;

-- 貸付・貯蓄の残高が「いつ、なぜ変わったか」を保存する共通取引履歴。
CREATE TABLE IF NOT EXISTS employee_finance_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    account_reference_id BIGINT NOT NULL,
    daily_report_id BIGINT NULL,
    transaction_date DATE NOT NULL,
    amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    balance_before DECIMAL(12, 2) NOT NULL DEFAULT 0,
    balance_after DECIMAL(12, 2) NOT NULL DEFAULT 0,
    note VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_employee_finance_transaction_employee
        (tenant_id, employee_id, transaction_date),
    INDEX idx_employee_finance_transaction_source
        (tenant_id, daily_report_id)
);

-- 既存残高は、履歴導入時点の初期残高として1度だけ登録する。
INSERT INTO employee_finance_transaction (
    employee_id,
    account_type,
    transaction_type,
    account_reference_id,
    transaction_date,
    amount,
    balance_before,
    balance_after,
    note,
    created_at,
    updated_at,
    tenant_id
)
SELECT
    loan.employee_id,
    'LOAN',
    'OPENING_BALANCE',
    loan.id,
    COALESCE(loan.loan_date, CURRENT_DATE),
    loan.current_balance,
    0,
    loan.current_balance,
    '残高履歴導入時の貸付初期残高',
    NOW(6),
    NOW(6),
    loan.tenant_id
FROM employee_loan loan
WHERE loan.deleted_at IS NULL
  AND loan.current_balance <> 0
  AND NOT EXISTS (
      SELECT 1
      FROM employee_finance_transaction tx
      WHERE tx.tenant_id = loan.tenant_id
        AND tx.account_type = 'LOAN'
        AND tx.account_reference_id = loan.id
        AND tx.transaction_type = 'OPENING_BALANCE'
        AND tx.deleted_at IS NULL
  );

INSERT INTO employee_finance_transaction (
    employee_id,
    account_type,
    transaction_type,
    account_reference_id,
    transaction_date,
    amount,
    balance_before,
    balance_after,
    note,
    created_at,
    updated_at,
    tenant_id
)
SELECT
    saving.employee_id,
    'SAVING',
    'OPENING_BALANCE',
    saving.id,
    CURRENT_DATE,
    saving.current_balance,
    0,
    saving.current_balance,
    '残高履歴導入時の貯蓄初期残高',
    NOW(6),
    NOW(6),
    saving.tenant_id
FROM employee_saving saving
WHERE saving.deleted_at IS NULL
  AND saving.current_balance <> 0
  AND NOT EXISTS (
      SELECT 1
      FROM employee_finance_transaction tx
      WHERE tx.tenant_id = saving.tenant_id
        AND tx.account_type = 'SAVING'
        AND tx.account_reference_id = saving.id
        AND tx.transaction_type = 'OPENING_BALANCE'
        AND tx.deleted_at IS NULL
  );
