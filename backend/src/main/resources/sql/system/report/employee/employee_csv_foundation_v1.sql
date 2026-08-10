-- ProjectAdminSystem V1
-- 従業員一覧CSV（帳票基盤・履歴保存）

SET NAMES utf8mb4;
SET @tenant_id = 'default';
SET @now = CURRENT_TIMESTAMP(6);

CREATE TABLE IF NOT EXISTS employee_csv_input (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(100) NOT NULL,
    include_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_employee_csv_input_execution (tenant_id, execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS employee_csv_output (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(100) NOT NULL,
    employee_code VARCHAR(100) NOT NULL,
    employee_name VARCHAR(200) NOT NULL,
    employee_name_kana VARCHAR(200) NULL,
    gender VARCHAR(20) NULL,
    birth_date DATE NULL,
    hire_date DATE NULL,
    resign_date DATE NULL,
    employment_type VARCHAR(30) NOT NULL,
    employment_status VARCHAR(30) NOT NULL,
    phone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    postal_code VARCHAR(20) NULL,
    address VARCHAR(500) NULL,
    dormitory_flag BOOLEAN NOT NULL,
    dormitory_type VARCHAR(30) NULL,
    salary_type VARCHAR(30) NULL,
    payment_cycle VARCHAR(30) NULL,
    monthly_salary DECIMAL(12, 2) NULL,
    daily_wage DECIMAL(12, 2) NULL,
    hourly_wage DECIMAL(12, 2) NULL,
    active_flag BOOLEAN NOT NULL,
    deleted_flag BOOLEAN NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_employee_csv_output_execution (tenant_id, execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS sp_employee_csv_prepare;
DELIMITER $$

CREATE PROCEDURE sp_employee_csv_prepare(IN p_execution_id VARCHAR(100))
BEGIN
    DECLARE v_tenant_id VARCHAR(255);
    DECLARE v_include_deleted BOOLEAN DEFAULT FALSE;

    SELECT input.tenant_id, input.include_deleted
      INTO v_tenant_id, v_include_deleted
    FROM employee_csv_input input
    WHERE input.execution_id = p_execution_id
    ORDER BY input.id DESC
    LIMIT 1;

    IF v_tenant_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'employee csv input is missing';
    END IF;

    DELETE FROM employee_csv_output
    WHERE tenant_id = v_tenant_id
      AND execution_id = p_execution_id;

    INSERT INTO employee_csv_output (
        execution_id,
        employee_code, employee_name, employee_name_kana,
        gender, birth_date, hire_date, resign_date,
        employment_type, employment_status,
        phone, email, postal_code, address,
        dormitory_flag, dormitory_type,
        salary_type, payment_cycle,
        monthly_salary, daily_wage, hourly_wage,
        active_flag, deleted_flag,
        tenant_id, created_at, updated_at
    )
    SELECT
        p_execution_id,
        employee.employee_code,
        employee.employee_name,
        employee.employee_name_kana,
        employee.gender,
        employee.birth_date,
        employee.hire_date,
        employee.resign_date,
        employee.employment_type,
        employee.employment_status,
        employee.phone,
        employee.email,
        employee.postal_code,
        employee.address,
        employee.dormitory_flag,
        employee.dormitory_type,
        contract.salary_type,
        contract.payment_cycle,
        contract.monthly_salary,
        contract.daily_wage,
        contract.hourly_wage,
        employee.active_flag,
        employee.deleted_at IS NOT NULL,
        employee.tenant_id,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    FROM employee employee
    LEFT JOIN employee_contract contract
      ON contract.employee_id = employee.id
     AND contract.tenant_id = employee.tenant_id
     AND contract.deleted_at IS NULL
    WHERE employee.tenant_id = v_tenant_id
      AND (v_include_deleted = TRUE OR employee.deleted_at IS NULL)
    ORDER BY employee.employee_code;
END$$

DELIMITER ;

DROP PROCEDURE IF EXISTS sp_employee_csv_cleanup;
DELIMITER $$

CREATE PROCEDURE sp_employee_csv_cleanup(IN p_execution_id VARCHAR(100))
BEGIN
    DELETE FROM employee_csv_output
    WHERE execution_id = p_execution_id;

    DELETE FROM employee_csv_input
    WHERE execution_id = p_execution_id;
END$$

DELIMITER ;

INSERT INTO report_master (
    tenant_id, created_at, updated_at,
    report_code, report_name, template_file_name,
    work_table, input_table, output_table,
    source_view_name, history_table,
    pre_process_type, pre_process_sql, procedure_name, query_sql,
    cleanup_type, cleanup_sql, cleanup_procedure_name,
    layout_type, layout_count, file_name, output_format,
    use_signature, preview_enabled, active_flag
) VALUES (
    @tenant_id, @now, @now,
    'EMPLOYEE_CSV', '従業員CSV', NULL,
    'employee_csv', 'employee_csv_input', 'employee_csv_output',
    NULL, NULL,
    'PROCEDURE', NULL, 'sp_employee_csv_prepare',
    'SELECT
        employee_code AS `社員コード`,
        employee_name AS `氏名`,
        employee_name_kana AS `フリガナ`,
        gender AS `性別`,
        birth_date AS `生年月日`,
        hire_date AS `入社日`,
        resign_date AS `退職日`,
        employment_type AS `雇用形態`,
        employment_status AS `在籍状態`,
        phone AS `電話番号`,
        email AS `メールアドレス`,
        postal_code AS `郵便番号`,
        address AS `住所`,
        dormitory_flag AS `入寮区分`,
        dormitory_type AS `寮タイプ`,
        salary_type AS `給与形態`,
        payment_cycle AS `支払周期`,
        monthly_salary AS `月給`,
        daily_wage AS `日給`,
        hourly_wage AS `時給`,
        active_flag AS `有効`,
        deleted_flag AS `削除済み`
     FROM employee_csv_output
     WHERE execution_id = :executionId
     ORDER BY employee_code',
    'PROCEDURE', NULL, 'sp_employee_csv_cleanup',
    'SINGLE', 1, '従業員一覧', 'CSV',
    FALSE, FALSE, TRUE
)
ON DUPLICATE KEY UPDATE
    report_name = VALUES(report_name),
    work_table = VALUES(work_table),
    input_table = VALUES(input_table),
    output_table = VALUES(output_table),
    pre_process_type = VALUES(pre_process_type),
    procedure_name = VALUES(procedure_name),
    query_sql = VALUES(query_sql),
    cleanup_type = VALUES(cleanup_type),
    cleanup_procedure_name = VALUES(cleanup_procedure_name),
    file_name = VALUES(file_name),
    output_format = VALUES(output_format),
    preview_enabled = VALUES(preview_enabled),
    active_flag = TRUE,
    deleted_at = NULL,
    updated_at = VALUES(updated_at);

SET @report_master_id = (
    SELECT id
    FROM report_master
    WHERE tenant_id = @tenant_id
      AND report_code = 'EMPLOYEE_CSV'
    LIMIT 1
);

DELETE FROM report_param
WHERE report_master_id = @report_master_id;

INSERT INTO report_param (
    tenant_id, created_at, updated_at,
    report_master_id,
    param_name, param_label, param_type, control_type,
    required_flag, visible_flag, multiple_flag, filter_flag,
    default_value, placeholder, input_column_name,
    display_order, active_flag
) VALUES (
    @tenant_id, @now, @now,
    @report_master_id,
    'includeDeleted', '削除済みを含める', 'BOOLEAN', 'CHECKBOX',
    FALSE, TRUE, FALSE, TRUE,
    'false', NULL, 'include_deleted',
    1, TRUE
);

INSERT INTO batch_job_definition (
    tenant_id, created_at, updated_at,
    job_code, job_name, job_type, target_code,
    immediate_executable, schedule_enabled, schedule_type,
    cron_expression, active_flag, description
) VALUES (
    @tenant_id, @now, @now,
    'EXPORT_EMPLOYEE_CSV', '従業員CSV出力', 'REPORT', 'EMPLOYEE_CSV',
    TRUE, FALSE, 'NONE',
    NULL, TRUE,
    '従業員情報をUTF-8 BOM付きCSVで出力し、帳票履歴へ保存する'
)
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name),
    job_type = VALUES(job_type),
    target_code = VALUES(target_code),
    immediate_executable = VALUES(immediate_executable),
    schedule_enabled = VALUES(schedule_enabled),
    schedule_type = VALUES(schedule_type),
    active_flag = TRUE,
    description = VALUES(description),
    updated_at = VALUES(updated_at);
