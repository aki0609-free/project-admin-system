-- ProjectAdmin 月次給与明細 履歴・出力スナップショット基盤 V1
-- MySQL 8.x
--
-- 前提:
--   1. system/report/pay_slip/setup.sql を先に適用する。
--   2. monthly_pay_slip_view_foundation_v1.sql を先に適用する。
--   3. 本DDL適用だけでは既存report_masterを切り替えない。
--
-- 実行モード:
--   INITIAL / RECLOSE : 最新View -> history -> render output
--   RETRY             : 同一Versionのhistory -> render output

SET NAMES utf8mb4;

SET @closing_version_ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE monthly_pay_slip_input ADD COLUMN closing_version INT NULL AFTER employee_id',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_pay_slip_input'
      AND column_name = 'closing_version'
);
PREPARE closing_version_statement FROM @closing_version_ddl;
EXECUTE closing_version_statement;
DEALLOCATE PREPARE closing_version_statement;

SET @execution_mode_ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE monthly_pay_slip_input ADD COLUMN execution_mode VARCHAR(30) NULL AFTER closing_version',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_pay_slip_input'
      AND column_name = 'execution_mode'
);
PREPARE execution_mode_statement FROM @execution_mode_ddl;
EXECUTE execution_mode_statement;
DEALLOCATE PREPARE execution_mode_statement;

CREATE TABLE IF NOT EXISTS monthly_pay_slip_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_month DATE NOT NULL,
    closing_version INT NOT NULL,
    employee_id BIGINT NOT NULL,
    employee_code VARCHAR(100) NOT NULL,
    employee_name VARCHAR(200) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(255) NULL,
    business_key VARCHAR(255) NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,

    work_day_count INT NOT NULL DEFAULT 0,
    work_hours DECIMAL(10, 2) NOT NULL DEFAULT 0,
    overtime_hours DECIMAL(10, 2) NOT NULL DEFAULT 0,
    night_work_hours DECIMAL(10, 2) NOT NULL DEFAULT 0,
    holiday_work_hours DECIMAL(10, 2) NOT NULL DEFAULT 0,
    paid_leave_days DECIMAL(10, 2) NOT NULL DEFAULT 0,

    basic_salary DECIMAL(15, 2) NOT NULL DEFAULT 0,
    allowance_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    gross_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    health_standard_remuneration DECIMAL(15, 2) NULL,
    pension_standard_remuneration DECIMAL(15, 2) NULL,
    health_insurance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    child_care_contribution DECIMAL(15, 2) NOT NULL DEFAULT 0,
    pension_insurance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    employment_insurance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    social_insurance_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    taxable_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    income_tax DECIMAL(15, 2) NOT NULL DEFAULT 0,
    resident_tax DECIMAL(15, 2) NOT NULL DEFAULT 0,
    legal_deduction_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    other_deduction_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    deduction_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    advance_payment_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    saving_balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    loan_balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    legal_deposit_refund_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_payment_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,

    source_view_name VARCHAR(200) NOT NULL,
    source_execution_id VARCHAR(100) NOT NULL,
    fixed_at TIMESTAMP(6) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_monthly_pay_slip_history_version
        UNIQUE (
            tenant_id,
            target_month,
            closing_version,
            employee_id
        ),
    CONSTRAINT uk_monthly_pay_slip_history_business
        UNIQUE (
            tenant_id,
            target_month,
            closing_version,
            business_key
        ),
    INDEX idx_monthly_pay_slip_history_employee
        (tenant_id, employee_id, target_month, closing_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @monthly_history_saving_balance_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_pay_slip_history'
      AND column_name = 'saving_balance'
);
SET @monthly_history_saving_balance_sql := IF(
    @monthly_history_saving_balance_exists = 0,
    'ALTER TABLE monthly_pay_slip_history ADD COLUMN saving_balance DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER advance_payment_amount',
    'SELECT 1'
);
PREPARE statement FROM @monthly_history_saving_balance_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @monthly_history_loan_balance_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_pay_slip_history'
      AND column_name = 'loan_balance'
);
SET @monthly_history_loan_balance_sql := IF(
    @monthly_history_loan_balance_exists = 0,
    'ALTER TABLE monthly_pay_slip_history ADD COLUMN loan_balance DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER saving_balance',
    'SELECT 1'
);
PREPARE statement FROM @monthly_history_loan_balance_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @monthly_history_legal_refund_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_pay_slip_history'
      AND column_name = 'legal_deposit_refund_amount'
);
SET @monthly_history_legal_refund_sql := IF(
    @monthly_history_legal_refund_exists = 0,
    'ALTER TABLE monthly_pay_slip_history ADD COLUMN legal_deposit_refund_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER loan_balance',
    'SELECT 1'
);
PREPARE statement FROM @monthly_history_legal_refund_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS monthly_pay_slip_history_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    monthly_pay_slip_history_id BIGINT NOT NULL,
    item_category VARCHAR(30) NOT NULL,
    item_no INT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    item_value DECIMAL(15, 2) NOT NULL DEFAULT 0,
    display_order INT NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_monthly_pay_slip_history_item
        UNIQUE (
            tenant_id,
            monthly_pay_slip_history_id,
            item_category,
            item_no
        ),
    CONSTRAINT fk_monthly_pay_slip_history_item_header
        FOREIGN KEY (monthly_pay_slip_history_id)
        REFERENCES monthly_pay_slip_history (id),
    INDEX idx_monthly_pay_slip_history_item_code
        (tenant_id, item_category, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_pay_slip_render_output (
    id BIGINT NOT NULL AUTO_INCREMENT,
    execution_id VARCHAR(100) NOT NULL,
    monthly_pay_slip_history_id BIGINT NOT NULL,
    target_month DATE NOT NULL,
    closing_version INT NOT NULL,
    employee_id BIGINT NOT NULL,
    employee_code VARCHAR(100) NOT NULL,
    employee_name VARCHAR(200) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    recipient_key VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(255) NULL,
    business_key VARCHAR(255) NOT NULL,
    mail_type VARCHAR(100) NULL,
    mail_template_key VARCHAR(100) NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,

    work_day_count INT NOT NULL DEFAULT 0,
    work_hours DECIMAL(10, 2) NOT NULL DEFAULT 0,
    overtime_hours DECIMAL(10, 2) NOT NULL DEFAULT 0,
    night_work_hours DECIMAL(10, 2) NOT NULL DEFAULT 0,
    holiday_work_hours DECIMAL(10, 2) NOT NULL DEFAULT 0,
    paid_leave_days DECIMAL(10, 2) NOT NULL DEFAULT 0,

    basic_salary DECIMAL(15, 2) NOT NULL DEFAULT 0,
    allowance_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    gross_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    health_standard_remuneration DECIMAL(15, 2) NULL,
    pension_standard_remuneration DECIMAL(15, 2) NULL,
    health_insurance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    child_care_contribution DECIMAL(15, 2) NOT NULL DEFAULT 0,
    pension_insurance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    employment_insurance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    social_insurance_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    taxable_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    income_tax DECIMAL(15, 2) NOT NULL DEFAULT 0,
    resident_tax DECIMAL(15, 2) NOT NULL DEFAULT 0,
    legal_deduction_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    other_deduction_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    deduction_total DECIMAL(15, 2) NOT NULL DEFAULT 0,
    advance_payment_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    saving_balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    loan_balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    legal_deposit_refund_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,
    net_payment_amount DECIMAL(15, 2) NOT NULL DEFAULT 0,

    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_monthly_pay_slip_render_output
        UNIQUE (tenant_id, execution_id, employee_id),
    CONSTRAINT fk_monthly_pay_slip_render_output_history
        FOREIGN KEY (monthly_pay_slip_history_id)
        REFERENCES monthly_pay_slip_history (id),
    INDEX idx_monthly_pay_slip_render_delivery
        (tenant_id, execution_id, business_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @monthly_output_saving_balance_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_pay_slip_render_output'
      AND column_name = 'saving_balance'
);
SET @monthly_output_saving_balance_sql := IF(
    @monthly_output_saving_balance_exists = 0,
    'ALTER TABLE monthly_pay_slip_render_output ADD COLUMN saving_balance DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER advance_payment_amount',
    'SELECT 1'
);
PREPARE statement FROM @monthly_output_saving_balance_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @monthly_output_loan_balance_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_pay_slip_render_output'
      AND column_name = 'loan_balance'
);
SET @monthly_output_loan_balance_sql := IF(
    @monthly_output_loan_balance_exists = 0,
    'ALTER TABLE monthly_pay_slip_render_output ADD COLUMN loan_balance DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER saving_balance',
    'SELECT 1'
);
PREPARE statement FROM @monthly_output_loan_balance_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @monthly_output_legal_refund_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_pay_slip_render_output'
      AND column_name = 'legal_deposit_refund_amount'
);
SET @monthly_output_legal_refund_sql := IF(
    @monthly_output_legal_refund_exists = 0,
    'ALTER TABLE monthly_pay_slip_render_output ADD COLUMN legal_deposit_refund_amount DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER loan_balance',
    'SELECT 1'
);
PREPARE statement FROM @monthly_output_legal_refund_sql;
EXECUTE statement;
DEALLOCATE PREPARE statement;

CREATE TABLE IF NOT EXISTS monthly_pay_slip_render_output_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    monthly_pay_slip_render_output_id BIGINT NOT NULL,
    item_category VARCHAR(30) NOT NULL,
    item_no INT NOT NULL,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    item_value DECIMAL(15, 2) NOT NULL DEFAULT 0,
    display_order INT NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_monthly_pay_slip_render_output_item
        UNIQUE (
            tenant_id,
            monthly_pay_slip_render_output_id,
            item_category,
            item_no
        ),
    CONSTRAINT fk_monthly_pay_slip_render_output_item_header
        FOREIGN KEY (monthly_pay_slip_render_output_id)
        REFERENCES monthly_pay_slip_render_output (id),
    INDEX idx_monthly_pay_slip_render_output_item_code
        (tenant_id, item_category, item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW vw_monthly_pay_slip_render_flat AS
SELECT
    output.*,
    COUNT(CASE WHEN item.item_category = 'ALLOWANCE' THEN 1 END)
        AS allowance_item_count,
    COUNT(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' THEN 1 END)
        AS legal_deduction_item_count,
    COUNT(CASE WHEN item.item_category = 'OTHER_DEDUCTION' THEN 1 END)
        AS other_deduction_item_count,

    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 1 THEN item.item_name END) AS allowance_item_name_01,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 1 THEN item.item_value END) AS allowance_item_value_01,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 2 THEN item.item_name END) AS allowance_item_name_02,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 2 THEN item.item_value END) AS allowance_item_value_02,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 3 THEN item.item_name END) AS allowance_item_name_03,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 3 THEN item.item_value END) AS allowance_item_value_03,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 4 THEN item.item_name END) AS allowance_item_name_04,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 4 THEN item.item_value END) AS allowance_item_value_04,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 5 THEN item.item_name END) AS allowance_item_name_05,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 5 THEN item.item_value END) AS allowance_item_value_05,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 6 THEN item.item_name END) AS allowance_item_name_06,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 6 THEN item.item_value END) AS allowance_item_value_06,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 7 THEN item.item_name END) AS allowance_item_name_07,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 7 THEN item.item_value END) AS allowance_item_value_07,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 8 THEN item.item_name END) AS allowance_item_name_08,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 8 THEN item.item_value END) AS allowance_item_value_08,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 9 THEN item.item_name END) AS allowance_item_name_09,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 9 THEN item.item_value END) AS allowance_item_value_09,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 10 THEN item.item_name END) AS allowance_item_name_10,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 10 THEN item.item_value END) AS allowance_item_value_10,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 11 THEN item.item_name END) AS allowance_item_name_11,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 11 THEN item.item_value END) AS allowance_item_value_11,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 12 THEN item.item_name END) AS allowance_item_name_12,
    MAX(CASE WHEN item.item_category = 'ALLOWANCE' AND item.item_no = 12 THEN item.item_value END) AS allowance_item_value_12,

    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 1 THEN item.item_name END) AS legal_item_name_01,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 1 THEN item.item_value END) AS legal_item_value_01,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 2 THEN item.item_name END) AS legal_item_name_02,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 2 THEN item.item_value END) AS legal_item_value_02,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 3 THEN item.item_name END) AS legal_item_name_03,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 3 THEN item.item_value END) AS legal_item_value_03,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 4 THEN item.item_name END) AS legal_item_name_04,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 4 THEN item.item_value END) AS legal_item_value_04,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 5 THEN item.item_name END) AS legal_item_name_05,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 5 THEN item.item_value END) AS legal_item_value_05,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 6 THEN item.item_name END) AS legal_item_name_06,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 6 THEN item.item_value END) AS legal_item_value_06,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 7 THEN item.item_name END) AS legal_item_name_07,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 7 THEN item.item_value END) AS legal_item_value_07,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 8 THEN item.item_name END) AS legal_item_name_08,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 8 THEN item.item_value END) AS legal_item_value_08,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 9 THEN item.item_name END) AS legal_item_name_09,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 9 THEN item.item_value END) AS legal_item_value_09,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 10 THEN item.item_name END) AS legal_item_name_10,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 10 THEN item.item_value END) AS legal_item_value_10,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 11 THEN item.item_name END) AS legal_item_name_11,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 11 THEN item.item_value END) AS legal_item_value_11,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 12 THEN item.item_name END) AS legal_item_name_12,
    MAX(CASE WHEN item.item_category = 'LEGAL_DEDUCTION' AND item.item_no = 12 THEN item.item_value END) AS legal_item_value_12,

    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 1 THEN item.item_name END) AS other_item_name_01,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 1 THEN item.item_value END) AS other_item_value_01,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 2 THEN item.item_name END) AS other_item_name_02,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 2 THEN item.item_value END) AS other_item_value_02,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 3 THEN item.item_name END) AS other_item_name_03,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 3 THEN item.item_value END) AS other_item_value_03,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 4 THEN item.item_name END) AS other_item_name_04,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 4 THEN item.item_value END) AS other_item_value_04,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 5 THEN item.item_name END) AS other_item_name_05,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 5 THEN item.item_value END) AS other_item_value_05,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 6 THEN item.item_name END) AS other_item_name_06,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 6 THEN item.item_value END) AS other_item_value_06,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 7 THEN item.item_name END) AS other_item_name_07,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 7 THEN item.item_value END) AS other_item_value_07,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 8 THEN item.item_name END) AS other_item_name_08,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 8 THEN item.item_value END) AS other_item_value_08,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 9 THEN item.item_name END) AS other_item_name_09,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 9 THEN item.item_value END) AS other_item_value_09,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 10 THEN item.item_name END) AS other_item_name_10,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 10 THEN item.item_value END) AS other_item_value_10,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 11 THEN item.item_name END) AS other_item_name_11,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 11 THEN item.item_value END) AS other_item_value_11,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 12 THEN item.item_name END) AS other_item_name_12,
    MAX(CASE WHEN item.item_category = 'OTHER_DEDUCTION' AND item.item_no = 12 THEN item.item_value END) AS other_item_value_12
FROM monthly_pay_slip_render_output output
LEFT JOIN monthly_pay_slip_render_output_item item
  ON item.monthly_pay_slip_render_output_id = output.id
 AND item.tenant_id = output.tenant_id
 AND item.deleted_at IS NULL
WHERE output.deleted_at IS NULL
GROUP BY output.id;

DROP PROCEDURE IF EXISTS sp_monthly_pay_slip_snapshot;

DELIMITER $$

CREATE PROCEDURE sp_monthly_pay_slip_snapshot(
    IN p_execution_id VARCHAR(100)
)
BEGIN
    DECLARE v_tenant_id VARCHAR(255);
    DECLARE v_target_month DATE;
    DECLARE v_closing_version INT;
    DECLARE v_execution_mode VARCHAR(30);
    DECLARE v_employee_id BIGINT;
    DECLARE v_source_count BIGINT DEFAULT 0;
    DECLARE v_not_ready_count BIGINT DEFAULT 0;
    DECLARE v_company_missing_count BIGINT DEFAULT 0;
    DECLARE v_overflow_count BIGINT DEFAULT 0;
    DECLARE v_history_count BIGINT DEFAULT 0;

    IF p_execution_id IS NULL OR TRIM(p_execution_id) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'execution_id is required';
    END IF;

    SELECT
        input.tenant_id,
        STR_TO_DATE(CONCAT(input.target_month, '-01'), '%Y-%m-%d'),
        input.closing_version,
        UPPER(input.execution_mode),
        input.employee_id
    INTO
        v_tenant_id,
        v_target_month,
        v_closing_version,
        v_execution_mode,
        v_employee_id
    FROM monthly_pay_slip_input input
    WHERE input.execution_id = p_execution_id
      AND input.deleted_at IS NULL
    ORDER BY input.id DESC
    LIMIT 1;

    IF v_tenant_id IS NULL
       OR v_target_month IS NULL
       OR v_closing_version IS NULL
       OR v_closing_version < 1
       OR v_execution_mode NOT IN ('INITIAL', 'RECLOSE', 'RETRY') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'invalid monthly pay slip input';
    END IF;

    SELECT COUNT(*)
    INTO v_source_count
    FROM vw_monthly_pay_slip_latest source
    WHERE source.tenant_id = v_tenant_id
      AND source.target_month = v_target_month
      AND (v_employee_id IS NULL OR source.employee_id = v_employee_id);

    IF v_source_count = 0 AND v_execution_mode <> 'RETRY' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'monthly pay slip source is empty';
    END IF;

    SELECT COUNT(*)
    INTO v_not_ready_count
    FROM vw_monthly_pay_slip_latest source
    WHERE source.tenant_id = v_tenant_id
      AND source.target_month = v_target_month
      AND (v_employee_id IS NULL OR source.employee_id = v_employee_id)
      AND source.calculation_ready = FALSE;

    IF v_not_ready_count > 0 AND v_execution_mode <> 'RETRY' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'monthly pay slip calculation master is not ready';
    END IF;

    SELECT COUNT(*)
    INTO v_company_missing_count
    FROM vw_monthly_pay_slip_latest source
    WHERE source.tenant_id = v_tenant_id
      AND source.target_month = v_target_month
      AND (v_employee_id IS NULL OR source.employee_id = v_employee_id)
      AND (
          source.company_name IS NULL
          OR TRIM(source.company_name) = ''
      );

    IF v_company_missing_count > 0
       AND v_execution_mode <> 'RETRY' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'monthly pay slip company profile is missing';
    END IF;

    SELECT COUNT(*)
    INTO v_overflow_count
    FROM vw_monthly_pay_slip_latest source
    WHERE source.tenant_id = v_tenant_id
      AND source.target_month = v_target_month
      AND (v_employee_id IS NULL OR source.employee_id = v_employee_id)
      AND (
          source.allowance_item_count > 12
          OR source.legal_deduction_item_count > 12
          OR source.other_deduction_item_count > 12
      );

    IF v_overflow_count > 0 AND v_execution_mode <> 'RETRY' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'monthly pay slip item count exceeds 12';
    END IF;

    SELECT COUNT(*)
    INTO v_history_count
    FROM monthly_pay_slip_history history
    WHERE history.tenant_id = v_tenant_id
      AND history.target_month = v_target_month
      AND history.closing_version = v_closing_version
      AND history.deleted_at IS NULL
      AND (v_employee_id IS NULL OR history.employee_id = v_employee_id);

    IF v_execution_mode IN ('INITIAL', 'RECLOSE')
       AND v_history_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'monthly pay slip history version already exists';
    END IF;

    IF v_execution_mode = 'RETRY'
       AND v_history_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'monthly pay slip history version does not exist';
    END IF;

    IF v_execution_mode IN ('INITIAL', 'RECLOSE') THEN
        INSERT INTO monthly_pay_slip_history (
            target_month,
            closing_version,
            employee_id,
            employee_code,
            employee_name,
            company_name,
            recipient_email,
            business_key,
            period_from,
            period_to,
            work_day_count,
            work_hours,
            overtime_hours,
            night_work_hours,
            holiday_work_hours,
            paid_leave_days,
            basic_salary,
            allowance_total,
            gross_amount,
            health_standard_remuneration,
            pension_standard_remuneration,
            health_insurance,
            child_care_contribution,
            pension_insurance,
            employment_insurance,
            social_insurance_total,
            taxable_amount,
            income_tax,
            resident_tax,
            legal_deduction_total,
            other_deduction_total,
            deduction_total,
            advance_payment_amount,
            saving_balance,
            loan_balance,
            legal_deposit_refund_amount,
            net_payment_amount,
            source_view_name,
            source_execution_id,
            fixed_at,
            tenant_id,
            created_at,
            updated_at
        )
        SELECT
            source.target_month,
            v_closing_version,
            source.employee_id,
            source.employee_code,
            source.employee_name,
            source.company_name,
            source.recipient_email,
            source.business_key,
            source.period_from,
            source.period_to,
            source.work_day_count,
            source.work_hours,
            source.overtime_hours,
            source.night_work_hours,
            source.holiday_work_hours,
            source.paid_leave_days,
            source.basic_salary,
            source.allowance_total,
            source.gross_amount,
            source.health_standard_remuneration,
            source.pension_standard_remuneration,
            source.health_insurance,
            source.child_care_contribution,
            source.pension_insurance,
            source.employment_insurance,
            source.social_insurance_total,
            source.taxable_amount,
            source.income_tax,
            source.resident_tax,
            source.legal_deduction_total,
            source.other_deduction_total,
            source.deduction_total,
            source.advance_payment_amount,
            source.saving_balance,
            source.loan_balance,
            source.legal_deposit_refund_amount,
            source.net_payment_amount,
            'vw_monthly_pay_slip_latest',
            p_execution_id,
            NOW(6),
            source.tenant_id,
            NOW(6),
            NOW(6)
        FROM vw_monthly_pay_slip_latest source
        WHERE source.tenant_id = v_tenant_id
          AND source.target_month = v_target_month
          AND (v_employee_id IS NULL OR source.employee_id = v_employee_id);

        INSERT INTO monthly_pay_slip_history_item (
            monthly_pay_slip_history_id,
            item_category,
            item_no,
            item_code,
            item_name,
            item_value,
            display_order,
            tenant_id,
            created_at,
            updated_at
        )
        SELECT
            history.id,
            item.item_category,
            item.item_no,
            item.item_code,
            item.item_name,
            item.item_value,
            item.display_order,
            item.tenant_id,
            NOW(6),
            NOW(6)
        FROM vw_monthly_pay_slip_variable_item item
        JOIN monthly_pay_slip_history history
          ON history.tenant_id = item.tenant_id
         AND history.target_month = item.target_month
         AND history.employee_id = item.employee_id
         AND history.closing_version = v_closing_version
         AND history.deleted_at IS NULL
        WHERE item.tenant_id = v_tenant_id
          AND item.target_month = v_target_month
          AND (v_employee_id IS NULL OR item.employee_id = v_employee_id);
    END IF;

    DELETE output_item
    FROM monthly_pay_slip_render_output_item output_item
    JOIN monthly_pay_slip_render_output output
      ON output.id = output_item.monthly_pay_slip_render_output_id
    WHERE output.tenant_id = v_tenant_id
      AND output.execution_id = p_execution_id;

    DELETE FROM monthly_pay_slip_render_output
    WHERE tenant_id = v_tenant_id
      AND execution_id = p_execution_id;

    INSERT INTO monthly_pay_slip_render_output (
        execution_id,
        monthly_pay_slip_history_id,
        target_month,
        closing_version,
        employee_id,
        employee_code,
        employee_name,
        company_name,
        recipient_key,
        recipient_name,
        recipient_email,
        business_key,
        mail_type,
        mail_template_key,
        period_from,
        period_to,
        work_day_count,
        work_hours,
        overtime_hours,
        night_work_hours,
        holiday_work_hours,
        paid_leave_days,
        basic_salary,
        allowance_total,
        gross_amount,
        health_standard_remuneration,
        pension_standard_remuneration,
        health_insurance,
        child_care_contribution,
        pension_insurance,
        employment_insurance,
        social_insurance_total,
        taxable_amount,
        income_tax,
        resident_tax,
        legal_deduction_total,
        other_deduction_total,
        deduction_total,
        advance_payment_amount,
        saving_balance,
        loan_balance,
        legal_deposit_refund_amount,
        net_payment_amount,
        tenant_id,
        created_at,
        updated_at
    )
    SELECT
        p_execution_id,
        history.id,
        history.target_month,
        history.closing_version,
        history.employee_id,
        history.employee_code,
        history.employee_name,
        history.company_name,
        CAST(history.employee_id AS CHAR),
        history.employee_name,
        history.recipient_email,
        history.business_key,
        'MONTHLY_PAY_SLIP',
        'MONTHLY_PAY_SLIP_NOTICE',
        history.period_from,
        history.period_to,
        history.work_day_count,
        history.work_hours,
        history.overtime_hours,
        history.night_work_hours,
        history.holiday_work_hours,
        history.paid_leave_days,
        history.basic_salary,
        history.allowance_total,
        history.gross_amount,
        history.health_standard_remuneration,
        history.pension_standard_remuneration,
        history.health_insurance,
        history.child_care_contribution,
        history.pension_insurance,
        history.employment_insurance,
        history.social_insurance_total,
        history.taxable_amount,
        history.income_tax,
        history.resident_tax,
        history.legal_deduction_total,
        history.other_deduction_total,
        history.deduction_total,
        history.advance_payment_amount,
        history.saving_balance,
        history.loan_balance,
        history.legal_deposit_refund_amount,
        history.net_payment_amount,
        history.tenant_id,
        NOW(6),
        NOW(6)
    FROM monthly_pay_slip_history history
    WHERE history.tenant_id = v_tenant_id
      AND history.target_month = v_target_month
      AND history.closing_version = v_closing_version
      AND history.deleted_at IS NULL
      AND (v_employee_id IS NULL OR history.employee_id = v_employee_id);

    INSERT INTO monthly_pay_slip_render_output_item (
        monthly_pay_slip_render_output_id,
        item_category,
        item_no,
        item_code,
        item_name,
        item_value,
        display_order,
        tenant_id,
        created_at,
        updated_at
    )
    SELECT
        output.id,
        history_item.item_category,
        history_item.item_no,
        history_item.item_code,
        history_item.item_name,
        history_item.item_value,
        history_item.display_order,
        output.tenant_id,
        NOW(6),
        NOW(6)
    FROM monthly_pay_slip_render_output output
    JOIN monthly_pay_slip_history_item history_item
      ON history_item.monthly_pay_slip_history_id = output.monthly_pay_slip_history_id
     AND history_item.tenant_id = output.tenant_id
     AND history_item.deleted_at IS NULL
    WHERE output.tenant_id = v_tenant_id
      AND output.execution_id = p_execution_id;

END$$

DELIMITER ;

DROP PROCEDURE IF EXISTS sp_monthly_pay_slip_cleanup;

DELIMITER $$

CREATE PROCEDURE sp_monthly_pay_slip_cleanup(
    IN p_execution_id VARCHAR(100)
)
BEGIN
    DELETE output_item
    FROM monthly_pay_slip_render_output_item output_item
    JOIN monthly_pay_slip_render_output output
      ON output.id = output_item.monthly_pay_slip_render_output_id
    WHERE output.execution_id = p_execution_id;

    DELETE FROM monthly_pay_slip_render_output
    WHERE execution_id = p_execution_id;

    DELETE FROM monthly_pay_slip_input
    WHERE execution_id = p_execution_id;
END$$

DELIMITER ;

-- 帳票レンダラーは次のViewをexecution_idで参照する。
--
-- JasperReports:
--   vw_monthly_pay_slip_render_flat
--
-- 内部ではheaderと可変itemsを正規化して保持し、JasperReportsへ渡す直前だけ
-- item_name_01..12 / item_value_01..12へフラット化する。
-- Syncfusion Spreadsheetではheader + category別itemsをWorkbook JSONへ変換する。
