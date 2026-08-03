-- ProjectAdminSystem V1
-- 労務費一覧表（月次・Excelテンプレート出力・履歴保存）
-- MySQL 8.x
--
-- 実行契約:
--   INITIAL / RECLOSE : 最新View -> history -> output
--   RETRY             : 同一Versionのhistory -> output
--
-- 支払日は closing_setting(setting_code='PAYROLL') を正とする。

SET NAMES utf8mb4;

CREATE OR REPLACE VIEW vw_monthly_labor_cost_list_item_total AS
SELECT
    item.tenant_id,
    item.target_month,
    item.employee_id,
    COALESCE(SUM(CASE
        WHEN item.item_category = 'ALLOWANCE'
         AND item.item_code = 'OVERTIME_PAY'
            THEN item.item_value ELSE 0 END), 0) AS overtime_pay_amount,
    COALESCE(SUM(CASE
        WHEN item.item_category = 'ALLOWANCE'
         AND item.item_code = 'NIGHT_PAY'
            THEN item.item_value ELSE 0 END), 0) AS night_pay_amount,
    COALESCE(SUM(CASE
        WHEN item.item_category = 'ALLOWANCE'
         AND item.item_code = 'DRIVER_ALLOWANCE'
            THEN item.item_value ELSE 0 END), 0) AS driver_allowance_amount,
    COALESCE(SUM(CASE
        WHEN item.item_category = 'ALLOWANCE'
         AND item.item_code IN (
             'BUSINESS_TRIP_ALLOWANCE',
             'TRIP_ALLOWANCE',
             'BUSINESS_TRIP'
         ) THEN item.item_value ELSE 0 END), 0)
        AS business_trip_allowance_amount,
    COALESCE(SUM(CASE
        WHEN item.item_category IN ('LEGAL_DEDUCTION', 'OTHER_DEDUCTION')
         AND item.item_code = 'YEAR_END_ADJUSTMENT'
            THEN item.item_value ELSE 0 END), 0)
        AS year_end_adjustment_amount,
    COALESCE(SUM(CASE
        WHEN item.item_category = 'OTHER_DEDUCTION'
         AND item.item_code = 'DORMITORY_FEE'
            THEN item.item_value ELSE 0 END), 0)
        AS dormitory_fee_amount,
    COALESCE(SUM(CASE
        WHEN item.item_category = 'OTHER_DEDUCTION'
         AND item.item_code = 'MOBILE_RENTAL'
            THEN item.item_value ELSE 0 END), 0)
        AS mobile_rental_amount,
    COALESCE(SUM(CASE
        WHEN item.item_category = 'OTHER_DEDUCTION'
         AND item.item_code = 'WIFI_FEE'
            THEN item.item_value ELSE 0 END), 0)
        AS wifi_fee_amount,
    COALESCE(SUM(CASE
        WHEN item.item_category IN ('LEGAL_DEDUCTION', 'OTHER_DEDUCTION')
         AND item.item_code NOT IN (
             'YEAR_END_ADJUSTMENT',
             'DORMITORY_FEE',
             'MOBILE_RENTAL',
             'WIFI_FEE',
             'ADVANCE_PAYMENT',
             'SAVING',
             'SAVINGS'
         ) THEN item.item_value ELSE 0 END), 0)
        AS other_deduction_amount
FROM vw_monthly_pay_slip_variable_item_source item
GROUP BY
    item.tenant_id,
    item.target_month,
    item.employee_id;

CREATE OR REPLACE VIEW vw_monthly_labor_cost_list_latest AS
SELECT
    source.tenant_id,
    source.target_month,
    source.employee_id,
    source.employee_code,
    source.employee_name,
    source.company_name,
    source.period_from,
    source.period_to,
    CASE payroll.payment_day_type
        WHEN 'END_OF_MONTH' THEN LAST_DAY(
            DATE_ADD(
                source.target_month,
                INTERVAL COALESCE(payroll.payment_month_offset, 0) MONTH
            )
        )
        WHEN 'DAY_OF_MONTH' THEN DATE_ADD(
            DATE_FORMAT(
                DATE_ADD(
                    source.target_month,
                    INTERVAL COALESCE(payroll.payment_month_offset, 0) MONTH
                ),
                '%Y-%m-01'
            ),
            INTERVAL (
                LEAST(
                    payroll.payment_day_value,
                    DAY(LAST_DAY(
                        DATE_ADD(
                            source.target_month,
                            INTERVAL COALESCE(payroll.payment_month_offset, 0) MONTH
                        )
                    ))
                ) - 1
            ) DAY
        )
        ELSE NULL
    END AS payment_date,
    CONCAT(
        'MONTHLY_LABOR_COST_LIST:',
        DATE_FORMAT(source.target_month, '%Y-%m'),
        ':',
        source.employee_id
    ) AS business_key,
    source.work_day_count,
    source.paid_leave_days,
    source.overtime_hours,
    source.night_work_hours,
    source.basic_salary,
    COALESCE(item.overtime_pay_amount, 0) AS overtime_pay_amount,
    COALESCE(item.night_pay_amount, 0) AS night_pay_amount,
    COALESCE(item.driver_allowance_amount, 0) AS driver_allowance_amount,
    source.allowance_total
        - COALESCE(item.overtime_pay_amount, 0)
        - COALESCE(item.night_pay_amount, 0)
        - COALESCE(item.driver_allowance_amount, 0)
        - COALESCE(item.business_trip_allowance_amount, 0)
        AS other_allowance_amount,
    COALESCE(item.business_trip_allowance_amount, 0)
        AS business_trip_allowance_amount,
    source.gross_amount,
    source.health_insurance,
    source.child_care_contribution,
    source.pension_insurance,
    source.employment_insurance,
    source.social_insurance_total,
    source.taxable_amount,
    source.income_tax,
    COALESCE(item.year_end_adjustment_amount, 0)
        AS year_end_adjustment_amount,
    source.resident_tax,
    COALESCE(item.dormitory_fee_amount, 0) AS dormitory_fee_amount,
    COALESCE(item.mobile_rental_amount, 0) AS mobile_rental_amount,
    COALESCE(item.wifi_fee_amount, 0) AS wifi_fee_amount,
    COALESCE(item.other_deduction_amount, 0) AS other_deduction_amount,
    source.social_insurance_total
        + source.income_tax
        + COALESCE(item.year_end_adjustment_amount, 0)
        + source.resident_tax
        + COALESCE(item.dormitory_fee_amount, 0)
        + COALESCE(item.mobile_rental_amount, 0)
        + COALESCE(item.wifi_fee_amount, 0)
        + COALESCE(item.other_deduction_amount, 0)
        AS deduction_total,
    source.gross_amount
        - (
            source.social_insurance_total
            + source.income_tax
            + COALESCE(item.year_end_adjustment_amount, 0)
            + source.resident_tax
            + COALESCE(item.dormitory_fee_amount, 0)
            + COALESCE(item.mobile_rental_amount, 0)
            + COALESCE(item.wifi_fee_amount, 0)
            + COALESCE(item.other_deduction_amount, 0)
        ) AS net_before_advance_amount,
    source.advance_payment_amount,
    attendance.saving_amount,
    source.gross_amount
        - (
            source.social_insurance_total
            + source.income_tax
            + COALESCE(item.year_end_adjustment_amount, 0)
            + source.resident_tax
            + COALESCE(item.dormitory_fee_amount, 0)
            + COALESCE(item.mobile_rental_amount, 0)
            + COALESCE(item.wifi_fee_amount, 0)
            + COALESCE(item.other_deduction_amount, 0)
            + source.advance_payment_amount
            + attendance.saving_amount
        ) AS net_payment_amount,
    source.calculation_ready,
    source.calculation_error_code
FROM vw_monthly_pay_slip_latest source
JOIN (
    SELECT setting.*
    FROM closing_setting setting
    JOIN (
        SELECT tenant_id, MAX(id) AS setting_id
        FROM closing_setting
        WHERE setting_code = 'PAYROLL'
          AND active_flag = TRUE
          AND deleted_at IS NULL
        GROUP BY tenant_id
    ) selected
      ON selected.setting_id = setting.id
     AND selected.tenant_id = setting.tenant_id
) payroll
  ON payroll.tenant_id = source.tenant_id
LEFT JOIN vw_monthly_labor_cost_list_item_total item
  ON item.tenant_id = source.tenant_id
 AND item.target_month = source.target_month
 AND item.employee_id = source.employee_id
JOIN vw_monthly_pay_slip_attendance attendance
  ON attendance.tenant_id = source.tenant_id
 AND attendance.target_month = source.target_month
 AND attendance.employee_id = source.employee_id;

CREATE TABLE IF NOT EXISTS monthly_labor_cost_list_input (
    id BIGINT NOT NULL AUTO_INCREMENT,
    execution_id VARCHAR(100) NOT NULL,
    target_month VARCHAR(7) NOT NULL,
    closing_version INT NOT NULL,
    execution_mode VARCHAR(30) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_monthly_labor_cost_list_input_execution
        (tenant_id, execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_labor_cost_list_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_month DATE NOT NULL,
    closing_version INT NOT NULL,
    employee_id BIGINT NOT NULL,
    employee_code VARCHAR(100) NOT NULL,
    employee_name VARCHAR(200) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    payment_date DATE NOT NULL,
    business_key VARCHAR(255) NOT NULL,
    work_day_count INT NOT NULL DEFAULT 0,
    paid_leave_days DECIMAL(10,2) NOT NULL DEFAULT 0,
    overtime_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    night_work_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    basic_salary DECIMAL(15,2) NOT NULL DEFAULT 0,
    overtime_pay_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    night_pay_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    driver_allowance_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    other_allowance_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    business_trip_allowance_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    gross_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    health_insurance DECIMAL(15,2) NOT NULL DEFAULT 0,
    child_care_contribution DECIMAL(15,2) NOT NULL DEFAULT 0,
    pension_insurance DECIMAL(15,2) NOT NULL DEFAULT 0,
    employment_insurance DECIMAL(15,2) NOT NULL DEFAULT 0,
    social_insurance_total DECIMAL(15,2) NOT NULL DEFAULT 0,
    taxable_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    income_tax DECIMAL(15,2) NOT NULL DEFAULT 0,
    year_end_adjustment_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    resident_tax DECIMAL(15,2) NOT NULL DEFAULT 0,
    dormitory_fee_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    mobile_rental_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    wifi_fee_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    other_deduction_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    deduction_total DECIMAL(15,2) NOT NULL DEFAULT 0,
    net_before_advance_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    advance_payment_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    saving_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    net_payment_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    source_view_name VARCHAR(200) NOT NULL,
    source_execution_id VARCHAR(100) NOT NULL,
    fixed_at TIMESTAMP(6) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_monthly_labor_cost_list_history
        UNIQUE (tenant_id, target_month, closing_version, employee_id),
    INDEX idx_monthly_labor_cost_list_history_business
        (tenant_id, target_month, closing_version, business_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_labor_cost_list_output
LIKE monthly_labor_cost_list_history;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE monthly_labor_cost_list_output ADD COLUMN execution_id VARCHAR(100) NOT NULL AFTER id',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_labor_cost_list_output'
      AND column_name = 'execution_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE monthly_labor_cost_list_output DROP INDEX uk_monthly_labor_cost_list_history',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_labor_cost_list_output'
      AND index_name = 'uk_monthly_labor_cost_list_history'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE monthly_labor_cost_list_output DROP INDEX idx_monthly_labor_cost_list_history_business',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_labor_cost_list_output'
      AND index_name = 'idx_monthly_labor_cost_list_history_business'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE monthly_labor_cost_list_output ADD CONSTRAINT uk_monthly_labor_cost_list_output UNIQUE (tenant_id, execution_id, employee_id)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_labor_cost_list_output'
      AND index_name = 'uk_monthly_labor_cost_list_output'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE monthly_labor_cost_list_output ADD INDEX idx_monthly_labor_cost_list_output_execution (tenant_id, execution_id, employee_code)',
        'SELECT 1'
    )
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'monthly_labor_cost_list_output'
      AND index_name = 'idx_monthly_labor_cost_list_output_execution'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP PROCEDURE IF EXISTS sp_monthly_labor_cost_list_snapshot;
DELIMITER $$

CREATE PROCEDURE sp_monthly_labor_cost_list_snapshot(
    IN p_execution_id VARCHAR(100)
)
BEGIN
    DECLARE v_tenant_id VARCHAR(255);
    DECLARE v_target_month DATE;
    DECLARE v_closing_version INT;
    DECLARE v_execution_mode VARCHAR(30);
    DECLARE v_source_count BIGINT DEFAULT 0;
    DECLARE v_not_ready_count BIGINT DEFAULT 0;
    DECLARE v_payment_date_missing_count BIGINT DEFAULT 0;
    DECLARE v_history_count BIGINT DEFAULT 0;

    SELECT
        input.tenant_id,
        STR_TO_DATE(CONCAT(input.target_month, '-01'), '%Y-%m-%d'),
        input.closing_version,
        UPPER(input.execution_mode)
    INTO
        v_tenant_id,
        v_target_month,
        v_closing_version,
        v_execution_mode
    FROM monthly_labor_cost_list_input input
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
            SET MESSAGE_TEXT = 'invalid monthly labor cost list input';
    END IF;

    IF v_execution_mode IN ('INITIAL', 'RECLOSE') THEN
        SELECT COUNT(*),
               COALESCE(SUM(source.calculation_ready = FALSE), 0),
               COALESCE(SUM(source.payment_date IS NULL), 0)
        INTO v_source_count, v_not_ready_count, v_payment_date_missing_count
        FROM vw_monthly_labor_cost_list_latest source
        WHERE source.tenant_id = v_tenant_id
          AND source.target_month = v_target_month;

        IF v_source_count = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'monthly labor cost list source is empty';
        END IF;
        IF v_not_ready_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'monthly payroll calculation master is not ready';
        END IF;
        IF v_payment_date_missing_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'payroll payment setting is missing';
        END IF;

        INSERT INTO monthly_labor_cost_list_history (
            target_month, closing_version,
            employee_id, employee_code, employee_name, company_name,
            period_from, period_to, payment_date, business_key,
            work_day_count, paid_leave_days, overtime_hours, night_work_hours,
            basic_salary, overtime_pay_amount, night_pay_amount,
            driver_allowance_amount, other_allowance_amount,
            business_trip_allowance_amount, gross_amount,
            health_insurance, child_care_contribution, pension_insurance,
            employment_insurance, social_insurance_total, taxable_amount,
            income_tax, year_end_adjustment_amount, resident_tax,
            dormitory_fee_amount, mobile_rental_amount, wifi_fee_amount,
            other_deduction_amount, deduction_total,
            net_before_advance_amount, advance_payment_amount,
            saving_amount, net_payment_amount,
            source_view_name, source_execution_id, fixed_at,
            tenant_id, created_at, updated_at
        )
        SELECT
            source.target_month, v_closing_version,
            source.employee_id, source.employee_code,
            source.employee_name, source.company_name,
            source.period_from, source.period_to,
            source.payment_date, source.business_key,
            source.work_day_count, source.paid_leave_days,
            source.overtime_hours, source.night_work_hours,
            source.basic_salary, source.overtime_pay_amount,
            source.night_pay_amount, source.driver_allowance_amount,
            source.other_allowance_amount,
            source.business_trip_allowance_amount, source.gross_amount,
            source.health_insurance, source.child_care_contribution,
            source.pension_insurance, source.employment_insurance,
            source.social_insurance_total, source.taxable_amount,
            source.income_tax, source.year_end_adjustment_amount,
            source.resident_tax, source.dormitory_fee_amount,
            source.mobile_rental_amount, source.wifi_fee_amount,
            source.other_deduction_amount, source.deduction_total,
            source.net_before_advance_amount,
            source.advance_payment_amount, source.saving_amount,
            source.net_payment_amount,
            'vw_monthly_labor_cost_list_latest', p_execution_id, NOW(6),
            source.tenant_id, NOW(6), NOW(6)
        FROM vw_monthly_labor_cost_list_latest source
        WHERE source.tenant_id = v_tenant_id
          AND source.target_month = v_target_month;
    END IF;

    SELECT COUNT(*)
    INTO v_history_count
    FROM monthly_labor_cost_list_history history
    WHERE history.tenant_id = v_tenant_id
      AND history.target_month = v_target_month
      AND history.closing_version = v_closing_version
      AND history.deleted_at IS NULL;

    IF v_history_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'monthly labor cost list history is empty';
    END IF;

    DELETE FROM monthly_labor_cost_list_output
    WHERE tenant_id = v_tenant_id
      AND execution_id = p_execution_id;

    INSERT INTO monthly_labor_cost_list_output (
        execution_id,
        target_month, closing_version,
        employee_id, employee_code, employee_name, company_name,
        period_from, period_to, payment_date, business_key,
        work_day_count, paid_leave_days, overtime_hours, night_work_hours,
        basic_salary, overtime_pay_amount, night_pay_amount,
        driver_allowance_amount, other_allowance_amount,
        business_trip_allowance_amount, gross_amount,
        health_insurance, child_care_contribution, pension_insurance,
        employment_insurance, social_insurance_total, taxable_amount,
        income_tax, year_end_adjustment_amount, resident_tax,
        dormitory_fee_amount, mobile_rental_amount, wifi_fee_amount,
        other_deduction_amount, deduction_total,
        net_before_advance_amount, advance_payment_amount,
        saving_amount, net_payment_amount,
        source_view_name, source_execution_id, fixed_at,
        tenant_id, created_at, updated_at
    )
    SELECT
        p_execution_id,
        history.target_month, history.closing_version,
        history.employee_id, history.employee_code,
        history.employee_name, history.company_name,
        history.period_from, history.period_to,
        history.payment_date, history.business_key,
        history.work_day_count, history.paid_leave_days,
        history.overtime_hours, history.night_work_hours,
        history.basic_salary, history.overtime_pay_amount,
        history.night_pay_amount, history.driver_allowance_amount,
        history.other_allowance_amount,
        history.business_trip_allowance_amount, history.gross_amount,
        history.health_insurance, history.child_care_contribution,
        history.pension_insurance, history.employment_insurance,
        history.social_insurance_total, history.taxable_amount,
        history.income_tax, history.year_end_adjustment_amount,
        history.resident_tax, history.dormitory_fee_amount,
        history.mobile_rental_amount, history.wifi_fee_amount,
        history.other_deduction_amount, history.deduction_total,
        history.net_before_advance_amount,
        history.advance_payment_amount, history.saving_amount,
        history.net_payment_amount,
        history.source_view_name, history.source_execution_id,
        history.fixed_at,
        history.tenant_id, NOW(6), NOW(6)
    FROM monthly_labor_cost_list_history history
    WHERE history.tenant_id = v_tenant_id
      AND history.target_month = v_target_month
      AND history.closing_version = v_closing_version
      AND history.deleted_at IS NULL;
END$$

DELIMITER ;

DROP PROCEDURE IF EXISTS sp_monthly_labor_cost_list_cleanup;
DELIMITER $$

CREATE PROCEDURE sp_monthly_labor_cost_list_cleanup(
    IN p_execution_id VARCHAR(100)
)
BEGIN
    DELETE FROM monthly_labor_cost_list_output
    WHERE execution_id = p_execution_id;

    DELETE FROM monthly_labor_cost_list_input
    WHERE execution_id = p_execution_id;
END$$

DELIMITER ;

SET @tenant_id = 'default';
SET @now = NOW(6);

INSERT INTO report_master (
    tenant_id, created_at, updated_at,
    report_code, report_name, template_file_name,
    work_table, input_table, output_table,
    source_view_name, history_table,
    pre_process_type, procedure_name, query_sql,
    cleanup_type, cleanup_procedure_name,
    layout_type, layout_count, file_name, output_format,
    use_signature, preview_enabled, active_flag
) VALUES (
    @tenant_id, @now, @now,
    'MONTHLY_LABOR_COST_LIST', '労務費一覧表',
    'monthly_labor_cost_list.xlsx',
    'monthly_labor_cost_list',
    'monthly_labor_cost_list_input',
    'monthly_labor_cost_list_output',
    'vw_monthly_labor_cost_list_latest',
    'monthly_labor_cost_list_history',
    'PROCEDURE', 'sp_monthly_labor_cost_list_snapshot',
    'SELECT * FROM monthly_labor_cost_list_output WHERE execution_id = :executionId ORDER BY employee_code',
    'PROCEDURE', 'sp_monthly_labor_cost_list_cleanup',
    'SINGLE', 1, '労務費一覧表_${targetMonth}', 'EXCEL',
    FALSE, TRUE, TRUE
)
ON DUPLICATE KEY UPDATE
    report_name = VALUES(report_name),
    template_file_name = VALUES(template_file_name),
    work_table = VALUES(work_table),
    input_table = VALUES(input_table),
    output_table = VALUES(output_table),
    source_view_name = VALUES(source_view_name),
    history_table = VALUES(history_table),
    pre_process_type = VALUES(pre_process_type),
    procedure_name = VALUES(procedure_name),
    query_sql = VALUES(query_sql),
    cleanup_type = VALUES(cleanup_type),
    cleanup_procedure_name = VALUES(cleanup_procedure_name),
    file_name = VALUES(file_name),
    output_format = VALUES(output_format),
    active_flag = VALUES(active_flag),
    updated_at = VALUES(updated_at);

SET @report_master_id = (
    SELECT id FROM report_master
    WHERE report_code = 'MONTHLY_LABOR_COST_LIST'
    LIMIT 1
);

DELETE FROM report_param
WHERE report_master_id = @report_master_id;

INSERT INTO report_param (
    tenant_id, created_at, updated_at, report_master_id,
    param_name, param_label, param_type, control_type,
    required_flag, visible_flag, multiple_flag, filter_flag,
    default_value, placeholder, input_column_name,
    display_order, active_flag
) VALUES
(
    @tenant_id, @now, @now, @report_master_id,
    'targetMonth', '対象月', 'STRING', 'TEXT',
    TRUE, TRUE, FALSE, TRUE,
    NULL, '対象月を選択', 'target_month', 1, TRUE
),
(
    @tenant_id, @now, @now, @report_master_id,
    'closingVersion', '締めVersion', 'LONG', 'NUMBER',
    TRUE, FALSE, FALSE, FALSE,
    NULL, NULL, 'closing_version', 2, TRUE
),
(
    @tenant_id, @now, @now, @report_master_id,
    'executionMode', '実行モード', 'STRING', 'TEXT',
    TRUE, FALSE, FALSE, FALSE,
    NULL, NULL, 'execution_mode', 3, TRUE
);

INSERT INTO monthly_closing_output_definition (
    output_type, output_code, execution_order,
    required_flag, active_flag, backup_retention_years,
    tenant_id, created_at, updated_at
) VALUES (
    'REPORT', 'MONTHLY_LABOR_COST_LIST', 15,
    TRUE, TRUE, 7,
    @tenant_id, @now, @now
)
ON DUPLICATE KEY UPDATE
    execution_order = VALUES(execution_order),
    required_flag = VALUES(required_flag),
    active_flag = VALUES(active_flag),
    backup_retention_years = VALUES(backup_retention_years),
    updated_at = VALUES(updated_at);

INSERT INTO batch_job_definition (
    tenant_id, job_code, job_name, job_type, target_code,
    immediate_executable, schedule_enabled, schedule_type,
    cron_expression, active_flag, description,
    created_at, updated_at
) VALUES (
    @tenant_id,
    'PRINT_MONTHLY_LABOR_COST_LIST',
    '労務費一覧表出力',
    'REPORT',
    'MONTHLY_LABOR_COST_LIST',
    TRUE, FALSE, 'NONE', NULL, TRUE,
    '月次締めVersionの履歴から労務費一覧Excelを生成する',
    @now, @now
)
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name),
    job_type = VALUES(job_type),
    target_code = VALUES(target_code),
    immediate_executable = VALUES(immediate_executable),
    schedule_enabled = VALUES(schedule_enabled),
    schedule_type = VALUES(schedule_type),
    cron_expression = VALUES(cron_expression),
    active_flag = VALUES(active_flag),
    description = VALUES(description),
    updated_at = VALUES(updated_at);

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE operation_report_preview ADD COLUMN report_name VARCHAR(200) NULL AFTER report_code',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_report_preview'
      AND column_name = 'report_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE operation_report_preview ADD COLUMN filter_column_name VARCHAR(100) NULL AFTER table_name',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_report_preview'
      AND column_name = 'filter_column_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE operation_report_preview
    MODIFY COLUMN output_type VARCHAR(30) NOT NULL;

INSERT INTO operation_report_preview (
    tenant_id, created_at, updated_at,
    operation_type, report_code, report_name, job_code,
    table_name, filter_column_name,
    template_name, order_by, display_order,
    active_flag, output_type
) VALUES (
    @tenant_id, @now, @now,
    'MONTHLY',
    'MONTHLY_LABOR_COST_LIST',
    '労務費一覧表',
    'PRINT_MONTHLY_LABOR_COST_LIST',
    'vw_monthly_labor_cost_list_latest',
    'target_month',
    'monthly_labor_cost_list.xlsx',
    'employee_code',
    15,
    TRUE,
    'EXCEL'
)
ON DUPLICATE KEY UPDATE
    report_name = VALUES(report_name),
    job_code = VALUES(job_code),
    table_name = VALUES(table_name),
    filter_column_name = VALUES(filter_column_name),
    template_name = VALUES(template_name),
    order_by = VALUES(order_by),
    display_order = VALUES(display_order),
    active_flag = VALUES(active_flag),
    output_type = VALUES(output_type),
    updated_at = VALUES(updated_at);
