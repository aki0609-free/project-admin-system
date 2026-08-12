-- ProjectAdminSystem V1 / Fuyo
-- 日次の法定預り金を月次締めで返金するための履歴基盤。
-- 日次の預り入金は daily_report_deductions を正本とし、
-- 返金だけを締めVersion付き取引として保存する。

SET NAMES utf8mb4;

-- 空DBおよび既存環境の両方で勤務態度手当を利用できるようにする。
INSERT INTO allowance_masters (
    allowance_code,
    allowance_name,
    allowance_type,
    calculation_type,
    allowance_unit,
    detail_view_type,
    rule_name,
    default_amount,
    allow_manual_input,
    min_amount,
    max_amount,
    taxable,
    show_on_daily_statement,
    show_on_monthly_statement,
    display_order,
    enabled,
    note,
    tenant_id,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    'ATTENDANCE_ATTITUDE',
    '勤務態度手当',
    'COMPANY',
    'MANUAL',
    'BOTH',
    'NONE',
    NULL,
    NULL,
    TRUE,
    0,
    NULL,
    TRUE,
    TRUE,
    TRUE,
    10,
    TRUE,
    '日報で勤務態度に応じた金額を手入力し、月次では日次確定額を集計する',
    'default',
    NOW(6),
    NOW(6),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM allowance_masters existing
    WHERE existing.allowance_code = 'ATTENDANCE_ATTITUDE'
      AND existing.deleted_at IS NULL
);

-- Fuyoの勤務態度手当は日報で金額を手入力し、月次では日次確定額を集計する。
UPDATE allowance_masters
SET calculation_type = 'MANUAL',
    allowance_unit = 'BOTH',
    rule_name = NULL,
    default_amount = NULL,
    allow_manual_input = TRUE,
    min_amount = 0,
    show_on_daily_statement = TRUE,
    show_on_monthly_statement = TRUE,
    updated_at = NOW(6)
WHERE allowance_code = 'ATTENDANCE_ATTITUDE'
  AND deleted_at IS NULL;

-- 管理手当も日次明細に表示する。
UPDATE allowance_masters
SET show_on_daily_statement = TRUE,
    show_on_monthly_statement = TRUE,
    updated_at = NOW(6)
WHERE allowance_code = 'MANAGEMENT_ALLOWANCE'
  AND deleted_at IS NULL;

-- 法定預り金は税計算ではなく日報上の手入力項目とする。
UPDATE deduction_masters
SET calculation_type = 'MANUAL',
    rule_name = NULL,
    default_amount = NULL,
    allow_manual_input = TRUE,
    min_amount = 0,
    deduction_unit = 'DAILY',
    show_on_daily_statement = TRUE,
    show_on_monthly_statement = FALSE,
    carry_to_monthly_settlement = TRUE,
    updated_at = NOW(6)
WHERE deduction_code = 'LEGAL_DEPOSIT'
  AND tenant_id = 'default'
  AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS employee_legal_deposit_refund (
    id BIGINT NOT NULL AUTO_INCREMENT,
    monthly_closing_id BIGINT NOT NULL,
    target_month DATE NOT NULL,
    period_end DATE NOT NULL,
    closing_version INT NOT NULL,
    employee_id BIGINT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    superseded_at TIMESTAMP(6) NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_legal_deposit_refund_version_employee
        UNIQUE (tenant_id, monthly_closing_id, closing_version, employee_id),
    INDEX idx_legal_deposit_refund_balance
        (tenant_id, employee_id, period_end, status, deleted_at),
    CONSTRAINT fk_legal_deposit_refund_closing
        FOREIGN KEY (monthly_closing_id) REFERENCES monthly_closings (id),
    CONSTRAINT fk_legal_deposit_refund_employee
        FOREIGN KEY (employee_id) REFERENCES employee (id),
    CONSTRAINT chk_legal_deposit_refund_status
        CHECK (status IN ('ACTIVE', 'SUPERSEDED')),
    CONSTRAINT chk_legal_deposit_refund_amount
        CHECK (amount > 0),
    CONSTRAINT chk_legal_deposit_refund_target_month
        CHECK (DAY(target_month) = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW vw_employee_legal_deposit_balance AS
SELECT
    employee_source.tenant_id,
    employee_source.employee_id,
    COALESCE(deposit.deposit_amount, 0) AS deposit_amount,
    COALESCE(refund.refund_amount, 0) AS refund_amount,
    GREATEST(
        COALESCE(deposit.deposit_amount, 0)
        - COALESCE(refund.refund_amount, 0),
        0
    ) AS current_balance
FROM (
    SELECT tenant_id, id AS employee_id
    FROM employee
    WHERE deleted_at IS NULL
) employee_source
LEFT JOIN (
    SELECT
        dr.tenant_id,
        dr.employee_id,
        SUM(drd.amount) AS deposit_amount
    FROM daily_report dr
    JOIN daily_report_deductions drd
      ON drd.tenant_id = dr.tenant_id
     AND drd.daily_report_id = dr.id
     AND drd.deduction_code = 'LEGAL_DEPOSIT'
     AND drd.deleted_at IS NULL
    WHERE dr.deleted_at IS NULL
      AND dr.approval_status = 'APPROVED'
    GROUP BY dr.tenant_id, dr.employee_id
) deposit
  ON deposit.tenant_id = employee_source.tenant_id
 AND deposit.employee_id = employee_source.employee_id
LEFT JOIN (
    SELECT tenant_id, employee_id, SUM(amount) AS refund_amount
    FROM employee_legal_deposit_refund
    WHERE status = 'ACTIVE'
      AND deleted_at IS NULL
    GROUP BY tenant_id, employee_id
) refund
  ON refund.tenant_id = employee_source.tenant_id
 AND refund.employee_id = employee_source.employee_id;
