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
    -- 既存DBとの作業テーブル互換用。CSVには出力しない。
    dormitory_flag BOOLEAN NOT NULL DEFAULT FALSE,
    dormitory_type VARCHAR(30) NULL,
    tax_category VARCHAR(30) NULL,
    tax_dependent_count INT NULL,
    paid_leave_remaining_days DECIMAL(5, 2) NULL,
    income_tax_calc_flag BOOLEAN NULL,
    resident_tax_calc_flag BOOLEAN NULL,
    resident_tax_monthly DECIMAL(12, 2) NULL,
    employment_insurance_flag BOOLEAN NULL,
    social_insurance_flag BOOLEAN NULL,
    health_insurance_flag BOOLEAN NULL,
    pension_insurance_flag BOOLEAN NULL,
    care_insurance_flag BOOLEAN NULL,
    commute_allowance_monthly DECIMAL(12, 2) NULL,
    contract_start_date DATE NULL,
    contract_end_date DATE NULL,
    salary_type VARCHAR(30) NULL,
    payment_cycle VARCHAR(30) NULL,
    monthly_salary DECIMAL(12, 2) NULL,
    weekly_wage DECIMAL(12, 2) NULL,
    daily_wage DECIMAL(12, 2) NULL,
    hourly_wage DECIMAL(12, 2) NULL,
    standard_working_hours DECIMAL(5, 2) NULL,
    contract_note VARCHAR(1000) NULL,
    payroll_item_settings LONGTEXT NULL,
    active_flag BOOLEAN NOT NULL,
    deleted_flag BOOLEAN NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_employee_csv_output_execution (tenant_id, execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 旧版の作業テーブルが存在する環境にも、現在の従業員フォーム項目を安全に追加する。
DROP PROCEDURE IF EXISTS sp_employee_csv_ensure_column;
DELIMITER $$

CREATE PROCEDURE sp_employee_csv_ensure_column(
    IN p_column_name VARCHAR(64),
    IN p_column_definition VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'employee_csv_output'
          AND column_name = p_column_name
    ) THEN
        SET @employee_csv_ddl = CONCAT(
            'ALTER TABLE employee_csv_output ADD COLUMN `',
            REPLACE(p_column_name, '`', '``'),
            '` ', p_column_definition
        );
        PREPARE employee_csv_statement FROM @employee_csv_ddl;
        EXECUTE employee_csv_statement;
        DEALLOCATE PREPARE employee_csv_statement;
    END IF;
END$$

DELIMITER ;

CALL sp_employee_csv_ensure_column('tax_category', 'VARCHAR(30) NULL');
CALL sp_employee_csv_ensure_column('tax_dependent_count', 'INT NULL');
CALL sp_employee_csv_ensure_column('paid_leave_remaining_days', 'DECIMAL(5, 2) NULL');
CALL sp_employee_csv_ensure_column('income_tax_calc_flag', 'BOOLEAN NULL');
CALL sp_employee_csv_ensure_column('resident_tax_calc_flag', 'BOOLEAN NULL');
CALL sp_employee_csv_ensure_column('resident_tax_monthly', 'DECIMAL(12, 2) NULL');
CALL sp_employee_csv_ensure_column('employment_insurance_flag', 'BOOLEAN NULL');
CALL sp_employee_csv_ensure_column('social_insurance_flag', 'BOOLEAN NULL');
CALL sp_employee_csv_ensure_column('health_insurance_flag', 'BOOLEAN NULL');
CALL sp_employee_csv_ensure_column('pension_insurance_flag', 'BOOLEAN NULL');
CALL sp_employee_csv_ensure_column('care_insurance_flag', 'BOOLEAN NULL');
CALL sp_employee_csv_ensure_column('commute_allowance_monthly', 'DECIMAL(12, 2) NULL');
CALL sp_employee_csv_ensure_column('contract_start_date', 'DATE NULL');
CALL sp_employee_csv_ensure_column('contract_end_date', 'DATE NULL');
CALL sp_employee_csv_ensure_column('weekly_wage', 'DECIMAL(12, 2) NULL');
CALL sp_employee_csv_ensure_column('standard_working_hours', 'DECIMAL(5, 2) NULL');
CALL sp_employee_csv_ensure_column('contract_note', 'VARCHAR(1000) NULL');
CALL sp_employee_csv_ensure_column('payroll_item_settings', 'LONGTEXT NULL');

DROP PROCEDURE sp_employee_csv_ensure_column;

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
        tax_category, tax_dependent_count, paid_leave_remaining_days,
        income_tax_calc_flag, resident_tax_calc_flag, resident_tax_monthly,
        employment_insurance_flag, social_insurance_flag,
        health_insurance_flag, pension_insurance_flag, care_insurance_flag,
        commute_allowance_monthly,
        contract_start_date, contract_end_date,
        salary_type, payment_cycle,
        monthly_salary, weekly_wage, daily_wage, hourly_wage,
        standard_working_hours, contract_note, payroll_item_settings,
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
        profile.tax_category,
        profile.tax_dependent_count,
        profile.paid_leave_remaining_days,
        profile.income_tax_calc_flag,
        profile.resident_tax_calc_flag,
        profile.resident_tax_monthly,
        profile.employment_insurance_flag,
        profile.social_insurance_flag,
        profile.health_insurance_flag,
        profile.pension_insurance_flag,
        profile.care_insurance_flag,
        profile.commute_allowance_monthly,
        contract.contract_start_date,
        contract.contract_end_date,
        contract.salary_type,
        contract.payment_cycle,
        contract.monthly_salary,
        contract.weekly_wage,
        contract.daily_wage,
        contract.hourly_wage,
        contract.standard_working_hours,
        contract.note,
        item_setting.payroll_item_settings,
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
    LEFT JOIN employee_payroll_profile profile
      ON profile.employee_id = employee.id
     AND profile.tenant_id = employee.tenant_id
     AND profile.deleted_at IS NULL
    LEFT JOIN (
        SELECT
            enrollment.tenant_id,
            enrollment.employee_id,
            CAST(JSON_ARRAYAGG(JSON_OBJECT(
                '種別', CASE policy.target_type
                    WHEN 'ALLOWANCE' THEN '手当'
                    WHEN 'DEDUCTION' THEN '控除'
                    ELSE policy.target_type
                END,
                'コード', policy.target_code,
                '名称', policy.display_name,
                '適用開始日', DATE_FORMAT(enrollment.effective_from, '%Y-%m-%d'),
                '適用終了日', enrollment.effective_to,
                '設定', enrollment.settings_json
            )) AS CHAR CHARACTER SET utf8mb4) AS payroll_item_settings
        FROM employee_payroll_item_enrollment enrollment
        JOIN payroll_item_balance_policy policy
          ON policy.id = enrollment.balance_policy_id
         AND policy.tenant_id = enrollment.tenant_id
         AND policy.application_scope = 'EMPLOYEE_ENROLLMENT'
         AND policy.active_flag = TRUE
         AND policy.deleted_at IS NULL
        WHERE enrollment.deleted_at IS NULL
          AND enrollment.effective_to IS NULL
        GROUP BY enrollment.tenant_id, enrollment.employee_id
    ) item_setting
      ON item_setting.tenant_id = employee.tenant_id
     AND item_setting.employee_id = employee.id
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
        CASE gender WHEN ''MALE'' THEN ''男性'' WHEN ''FEMALE'' THEN ''女性'' WHEN ''OTHER'' THEN ''その他'' ELSE gender END AS `性別`,
        birth_date AS `生年月日`,
        hire_date AS `入社日`,
        resign_date AS `退職日`,
        CASE employment_type WHEN ''FULL_TIME'' THEN ''正社員'' WHEN ''CONTRACT'' THEN ''契約社員'' WHEN ''PART_TIME'' THEN ''パート・アルバイト'' WHEN ''TEMPORARY'' THEN ''派遣社員'' WHEN ''DAILY_WORKER'' THEN ''日雇い'' ELSE employment_type END AS `雇用形態`,
        CASE employment_status WHEN ''ACTIVE'' THEN ''在籍'' WHEN ''LEAVE'' THEN ''休職'' WHEN ''RESIGNED'' THEN ''退職'' ELSE employment_status END AS `在籍状態`,
        phone AS `電話番号`,
        email AS `メールアドレス`,
        postal_code AS `郵便番号`,
        address AS `住所`,
        tax_category AS `税区分`,
        tax_dependent_count AS `扶養人数`,
        paid_leave_remaining_days AS `有給残日数`,
        CASE income_tax_calc_flag WHEN TRUE THEN ''対象'' WHEN FALSE THEN ''対象外'' END AS `所得税計算`,
        CASE resident_tax_calc_flag WHEN TRUE THEN ''対象'' WHEN FALSE THEN ''対象外'' END AS `住民税控除`,
        CASE employment_insurance_flag WHEN TRUE THEN ''対象'' WHEN FALSE THEN ''対象外'' END AS `雇用保険`,
        CASE social_insurance_flag WHEN TRUE THEN ''対象'' WHEN FALSE THEN ''対象外'' END AS `社会保険`,
        CASE health_insurance_flag WHEN TRUE THEN ''対象'' WHEN FALSE THEN ''対象外'' END AS `健康保険`,
        CASE pension_insurance_flag WHEN TRUE THEN ''対象'' WHEN FALSE THEN ''対象外'' END AS `厚生年金`,
        CASE care_insurance_flag WHEN TRUE THEN ''対象'' WHEN FALSE THEN ''対象外'' END AS `介護保険`,
        commute_allowance_monthly AS `通勤手当月額`,
        contract_start_date AS `契約開始日`,
        contract_end_date AS `契約終了日`,
        CASE salary_type WHEN ''MONTHLY'' THEN ''月給'' WHEN ''WEEKLY'' THEN ''週給'' WHEN ''DAILY'' THEN ''日給'' WHEN ''HOURLY'' THEN ''時給'' ELSE salary_type END AS `給与計算基準`,
        CASE payment_cycle WHEN ''DAILY'' THEN ''日払い'' WHEN ''WEEKLY'' THEN ''週払い'' WHEN ''MONTHLY'' THEN ''月払い'' ELSE payment_cycle END AS `支払サイクル`,
        monthly_salary AS `月給`,
        weekly_wage AS `週給`,
        daily_wage AS `日給`,
        hourly_wage AS `時給`,
        standard_working_hours AS `標準労働時間`,
        contract_note AS `契約メモ`,
        payroll_item_settings AS `従業員別手当・控除設定`,
        CASE active_flag WHEN TRUE THEN ''有効'' WHEN FALSE THEN ''無効'' END AS `有効状態`,
        CASE deleted_flag WHEN TRUE THEN ''削除済み'' WHEN FALSE THEN ''未削除'' END AS `削除状態`
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

-- 従業員データ取込はV1対象外。旧環境に定義が残っていても実行対象から外す。
UPDATE batch_job_definition
SET active_flag = FALSE,
    schedule_enabled = FALSE,
    schedule_type = 'NONE',
    cron_expression = NULL,
    updated_at = @now
WHERE job_code = 'IMPORT_EMPLOYEE'
  AND deleted_at IS NULL;
