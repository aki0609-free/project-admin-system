-- 月次注文書：顧客請求締めで確定した請求書履歴から生成する。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_order_form_input (
    id BIGINT NOT NULL AUTO_INCREMENT,
    execution_id VARCHAR(100) NOT NULL,
    target_month VARCHAR(7) NOT NULL,
    closing_version INT NOT NULL,
    execution_mode VARCHAR(30) NOT NULL,
    customer_id BIGINT NOT NULL,
    show_prime_contractor BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_order_form_input_execution (tenant_id, execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_order_form_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    monthly_invoice_history_id BIGINT NOT NULL,
    target_month DATE NOT NULL,
    closing_version INT NOT NULL,
    customer_id BIGINT NOT NULL,
    order_number VARCHAR(100) NOT NULL,
    order_date DATE NOT NULL,
    contract_from DATE NOT NULL,
    contract_to DATE NOT NULL,
    subject_text VARCHAR(500) NOT NULL,
    work_description VARCHAR(1000) NULL,
    subcontractor_name VARCHAR(255) NOT NULL,
    subcontractor_postal_code VARCHAR(20) NULL,
    subcontractor_address VARCHAR(1000) NULL,
    prime_contractor_name VARCHAR(255) NULL,
    prime_contractor_postal_code VARCHAR(20) NULL,
    prime_contractor_address VARCHAR(1000) NULL,
    show_prime_contractor BOOLEAN NOT NULL DEFAULT TRUE,
    tax_rate DECIMAL(7,4) NOT NULL,
    construction_price DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2) NOT NULL,
    contract_amount DECIMAL(15,2) NOT NULL,
    business_key VARCHAR(255) NOT NULL,
    source_execution_id VARCHAR(100) NOT NULL,
    fixed_at TIMESTAMP(6) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_order_form_history_version
        (tenant_id, target_month, closing_version, customer_id),
    UNIQUE KEY uk_monthly_order_form_history_business (tenant_id, business_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_order_form_render_execution (
    id BIGINT NOT NULL AUTO_INCREMENT,
    execution_id VARCHAR(100) NOT NULL,
    monthly_order_form_history_id BIGINT NOT NULL,
    recipient_key VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    business_key VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_order_form_render
        (tenant_id, execution_id, monthly_order_form_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW vw_monthly_order_form_render AS
SELECT
    render.execution_id,
    render.recipient_key,
    render.recipient_name,
    history.*
FROM monthly_order_form_render_execution render
JOIN monthly_order_form_history history
  ON history.id = render.monthly_order_form_history_id
 AND history.tenant_id = render.tenant_id
 AND history.deleted_at IS NULL
WHERE render.deleted_at IS NULL;

DROP PROCEDURE IF EXISTS sp_monthly_order_form_snapshot;
DELIMITER $$
CREATE PROCEDURE sp_monthly_order_form_snapshot(IN p_execution_id VARCHAR(100))
BEGIN
    DECLARE v_tenant_id VARCHAR(255);
    DECLARE v_target_month DATE;
    DECLARE v_closing_version INT;
    DECLARE v_execution_mode VARCHAR(30);
    DECLARE v_customer_id BIGINT;
    DECLARE v_show_prime BOOLEAN;
    DECLARE v_history_id BIGINT;

    SELECT input.tenant_id,
           STR_TO_DATE(CONCAT(input.target_month, '-01'), '%Y-%m-%d'),
           input.closing_version, UPPER(input.execution_mode),
           input.customer_id, input.show_prime_contractor
      INTO v_tenant_id, v_target_month, v_closing_version,
           v_execution_mode, v_customer_id, v_show_prime
      FROM monthly_order_form_input input
     WHERE input.execution_id = p_execution_id
       AND input.deleted_at IS NULL
     ORDER BY input.id DESC LIMIT 1;

    IF v_tenant_id IS NULL OR v_customer_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'monthly order form input is missing';
    END IF;

    IF v_execution_mode IN ('INITIAL', 'RECLOSE') THEN
        INSERT INTO monthly_order_form_history (
            monthly_invoice_history_id, target_month, closing_version,
            customer_id, order_number, order_date, contract_from, contract_to,
            subject_text, work_description,
            subcontractor_name, subcontractor_postal_code, subcontractor_address,
            prime_contractor_name, prime_contractor_postal_code,
            prime_contractor_address, show_prime_contractor,
            tax_rate, construction_price, tax_amount, contract_amount,
            business_key, source_execution_id, fixed_at,
            tenant_id, created_at, updated_at
        )
        SELECT invoice.id, invoice.target_month, invoice.closing_version,
               invoice.customer_id,
               CONCAT('ORD-', DATE_FORMAT(invoice.target_month, '%Y%m'), '-',
                      LPAD(invoice.customer_id, 6, '0'), '-V', invoice.closing_version),
               DATE_SUB(invoice.issue_date, INTERVAL 45 DAY),
               DATE_SUB(invoice.issue_date, INTERVAL 45 DAY),
               invoice.issue_date,
               CONCAT(DATE_FORMAT(invoice.target_month, '%Y年%m月'), ' 業務請負'),
               '作業内容は、口頭及び書面にて説明',
               invoice.company_name, invoice.company_postal_code,
               invoice.company_address,
               customer.name, customer.post_no, customer.address,
               COALESCE(v_show_prime, TRUE),
               invoice.tax_rate, invoice.subtotal_amount,
               invoice.tax_amount, invoice.total_amount,
               CONCAT('MONTHLY_ORDER_FORM:', DATE_FORMAT(invoice.target_month, '%Y-%m'),
                      ':', invoice.customer_id, ':V', invoice.closing_version),
               p_execution_id, NOW(6), invoice.tenant_id, NOW(6), NOW(6)
          FROM monthly_invoice_history invoice
          JOIN customers customer
            ON customer.id = invoice.customer_id
           AND customer.tenant_id = invoice.tenant_id
           AND customer.deleted_at IS NULL
         WHERE invoice.tenant_id = v_tenant_id
           AND invoice.target_month = v_target_month
           AND invoice.closing_version = v_closing_version
           AND invoice.customer_id = v_customer_id
           AND invoice.deleted_at IS NULL
         LIMIT 1;

        IF ROW_COUNT() <> 1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'confirmed monthly invoice is missing';
        END IF;
        SET v_history_id = LAST_INSERT_ID();
    ELSEIF v_execution_mode = 'RETRY' THEN
        SELECT history.id INTO v_history_id
          FROM monthly_order_form_history history
         WHERE history.tenant_id = v_tenant_id
           AND history.target_month = v_target_month
           AND history.closing_version = v_closing_version
           AND history.customer_id = v_customer_id
           AND history.deleted_at IS NULL
         LIMIT 1;
    ELSE
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'invalid monthly order form execution mode';
    END IF;

    DELETE FROM monthly_order_form_render_execution
     WHERE execution_id = p_execution_id;
    INSERT INTO monthly_order_form_render_execution (
        execution_id, monthly_order_form_history_id,
        recipient_key, recipient_name, business_key,
        tenant_id, created_at, updated_at
    )
    SELECT p_execution_id, history.id, CAST(history.customer_id AS CHAR),
           COALESCE(history.prime_contractor_name, ''), history.business_key,
           history.tenant_id, NOW(6), NOW(6)
      FROM monthly_order_form_history history
     WHERE history.id = v_history_id;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_monthly_order_form_cleanup;
DELIMITER $$
CREATE PROCEDURE sp_monthly_order_form_cleanup(IN p_execution_id VARCHAR(100))
BEGIN
    DELETE FROM monthly_order_form_render_execution WHERE execution_id = p_execution_id;
    DELETE FROM monthly_order_form_input WHERE execution_id = p_execution_id;
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
    'MONTHLY_ORDER_FORM', '月次注文書', 'monthly_order_form.jrxml',
    'monthly_order_form', 'monthly_order_form_input',
    'monthly_order_form_render_execution',
    'monthly_invoice_history', 'monthly_order_form_history',
    'PROCEDURE', 'sp_monthly_order_form_snapshot',
    'select * from vw_monthly_order_form_render where execution_id = :executionId',
    'PROCEDURE', 'sp_monthly_order_form_cleanup',
    'SINGLE', 1,
    '注文書_${targetMonth}_${customerId}_v${closingVersion}', 'PDF',
    FALSE, TRUE, TRUE
)
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at), report_name = VALUES(report_name),
    template_file_name = VALUES(template_file_name),
    input_table = VALUES(input_table), output_table = VALUES(output_table),
    source_view_name = VALUES(source_view_name), history_table = VALUES(history_table),
    procedure_name = VALUES(procedure_name), query_sql = VALUES(query_sql),
    cleanup_procedure_name = VALUES(cleanup_procedure_name),
    file_name = VALUES(file_name), preview_enabled = VALUES(preview_enabled),
    active_flag = VALUES(active_flag), deleted_at = NULL;

DELETE param FROM report_param param
JOIN report_master master ON master.id = param.report_master_id
WHERE master.tenant_id = @tenant_id AND master.report_code = 'MONTHLY_ORDER_FORM';

INSERT INTO report_param (
    tenant_id, created_at, updated_at, report_master_id,
    param_name, param_label, param_type, control_type,
    required_flag, visible_flag, multiple_flag, filter_flag,
    default_value, placeholder, input_column_name,
    display_order, active_flag
)
SELECT @tenant_id, @now, @now, master.id,
       definition.param_name, definition.param_label,
       definition.param_type, definition.control_type,
       definition.required_flag, definition.visible_flag,
       FALSE, definition.filter_flag, definition.default_value,
       definition.placeholder, definition.input_column_name,
       definition.display_order, TRUE
FROM report_master master
CROSS JOIN (
    SELECT 'targetMonth' param_name, '対象月' param_label, 'STRING' param_type,
           'TEXT' control_type, TRUE required_flag, FALSE visible_flag,
           TRUE filter_flag, NULL default_value, 'YYYY-MM' placeholder,
           'target_month' input_column_name, 1 display_order
    UNION ALL SELECT 'closingVersion', '締めVersion', 'LONG', 'NUMBER', TRUE, FALSE,
           FALSE, NULL, NULL, 'closing_version', 2
    UNION ALL SELECT 'executionMode', '実行モード', 'STRING', 'TEXT', TRUE, FALSE,
           FALSE, NULL, NULL, 'execution_mode', 3
    UNION ALL SELECT 'customerId', '顧客', 'LONG', 'TEXT', TRUE, FALSE,
           TRUE, NULL, NULL, 'customer_id', 4
    UNION ALL SELECT 'showPrimeContractor', '元請負人表示', 'BOOLEAN', 'CHECKBOX',
           TRUE, FALSE, FALSE, 'true', NULL, 'show_prime_contractor', 5
) definition
WHERE master.tenant_id = @tenant_id AND master.report_code = 'MONTHLY_ORDER_FORM';

INSERT INTO batch_job_definition (
    tenant_id, job_code, job_name, job_type, target_code,
    immediate_executable, schedule_enabled, schedule_type,
    cron_expression, active_flag, description, created_at, updated_at
) VALUES (
    @tenant_id, 'PRINT_MONTHLY_ORDER_FORM', '月次注文書出力',
    'REPORT', 'MONTHLY_ORDER_FORM', TRUE, FALSE, 'NONE', NULL, TRUE,
    '顧客請求締めで確定した請求書金額から注文書PDFを生成する', @now, @now
)
ON DUPLICATE KEY UPDATE job_name = VALUES(job_name), target_code = VALUES(target_code),
    active_flag = VALUES(active_flag), description = VALUES(description),
    updated_at = VALUES(updated_at);

INSERT INTO operation_report_preview (
    tenant_id, created_at, updated_at, operation_type,
    report_code, report_name, job_code, table_name,
    filter_column_name, target_param_name, template_name,
    html_template_key, html_template_version, order_by,
    display_order, active_flag, output_type
) VALUES (
    @tenant_id, @now, @now, 'MONTHLY',
    'MONTHLY_ORDER_FORM', '注文書', 'PRINT_MONTHLY_ORDER_FORM',
    'monthly_order_form_history', 'target_month', 'targetMonth',
    'monthly_order_form.jrxml', NULL, 1, 'customer_id',
    25, TRUE, 'PDF'
)
ON DUPLICATE KEY UPDATE report_name = VALUES(report_name), job_code = VALUES(job_code),
    table_name = VALUES(table_name), template_name = VALUES(template_name),
    display_order = VALUES(display_order), active_flag = VALUES(active_flag),
    output_type = VALUES(output_type), updated_at = VALUES(updated_at);

INSERT INTO monthly_closing_output_definition (
    output_type, output_code, execution_order,
    required_flag, active_flag, backup_retention_years,
    tenant_id, created_at, updated_at
) VALUES (
    'REPORT', 'MONTHLY_ORDER_FORM', 25,
    TRUE, TRUE, 7, @tenant_id, @now, @now
)
ON DUPLICATE KEY UPDATE
    execution_order = VALUES(execution_order),
    required_flag = VALUES(required_flag),
    active_flag = VALUES(active_flag),
    backup_retention_years = VALUES(backup_retention_years),
    updated_at = VALUES(updated_at),
    deleted_at = NULL;
