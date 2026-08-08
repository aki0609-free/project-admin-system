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
