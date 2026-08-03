-- ProjectAdminSystem V1
-- 作業証明伝票（翌日準備・Jasper PDF・履歴保存）
-- MySQL 8.x

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS daily_work_order_input (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    execution_id VARCHAR(100) NOT NULL,
    target_date DATE NOT NULL,
    INDEX idx_daily_work_order_input_execution (execution_id),
    INDEX idx_daily_work_order_input_target (tenant_id, target_date)
);

CREATE TABLE IF NOT EXISTS daily_work_order_render_output (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    execution_id VARCHAR(100) NOT NULL,
    slip_key VARCHAR(500) NOT NULL,
    target_date DATE NOT NULL,
    weekday_label VARCHAR(10) NULL,
    customer_id BIGINT NULL,
    customer_site_id BIGINT NULL,
    customer_name VARCHAR(255) NULL,
    site_name VARCHAR(255) NULL,
    page_no BIGINT NOT NULL,
    total_pages BIGINT NOT NULL,
    page_worker_count BIGINT NOT NULL,
    distance_from_company_km INT NULL,
    vehicle_count INT NULL,
    company_name VARCHAR(255) NULL,
    company_phone VARCHAR(50) NULL,
    company_fax VARCHAR(50) NULL,
    employee_name_1 VARCHAR(200) NULL,
    employee_name_2 VARCHAR(200) NULL,
    employee_name_3 VARCHAR(200) NULL,
    employee_name_4 VARCHAR(200) NULL,
    employee_name_5 VARCHAR(200) NULL,
    employee_name_6 VARCHAR(200) NULL,
    employee_name_7 VARCHAR(200) NULL,
    employee_name_8 VARCHAR(200) NULL,
    employee_name_9 VARCHAR(200) NULL,
    employee_name_10 VARCHAR(200) NULL,
    work_description_1 VARCHAR(1000) NULL,
    work_description_2 VARCHAR(1000) NULL,
    work_description_3 VARCHAR(1000) NULL,
    work_description_4 VARCHAR(1000) NULL,
    work_description_5 VARCHAR(1000) NULL,
    work_description_6 VARCHAR(1000) NULL,
    work_description_7 VARCHAR(1000) NULL,
    work_description_8 VARCHAR(1000) NULL,
    work_description_9 VARCHAR(1000) NULL,
    work_description_10 VARCHAR(1000) NULL,
    INDEX idx_daily_work_order_render_execution (execution_id),
    INDEX idx_daily_work_order_render_target (tenant_id, target_date),
    UNIQUE KEY uk_daily_work_order_render_page (execution_id, slip_key)
);

-- 帳票固有のデータ整形はこのViewへ閉じ込める。
-- Jasper側は1ページ分に平坦化されたoutputだけを描画する。
CREATE OR REPLACE VIEW vw_daily_work_order_render_source AS
WITH active_company AS (
    SELECT company.*,
           ROW_NUMBER() OVER (
               PARTITION BY company.tenant_id
               ORDER BY company.id
           ) AS company_row_no
    FROM company_profile company
    WHERE company.deleted_at IS NULL
      AND company.active_flag = TRUE
), ranked_assignment AS (
    SELECT
        preparation.tenant_id,
        preparation.target_date,
        assignment.customer_id,
        assignment.customer_site_id,
        assignment.customer_name,
        assignment.site_name,
        assignment.employee_id,
        assignment.employee_code,
        assignment.employee_name,
        assignment.work_description,
        dispatch.distance_from_company_km,
        dispatch.vehicle_count,
        company.company_name,
        company.phone AS company_phone,
        company.fax AS company_fax,
        ROW_NUMBER() OVER (
            PARTITION BY
                preparation.tenant_id,
                preparation.target_date,
                assignment.customer_id,
                assignment.customer_site_id
            ORDER BY assignment.employee_code, assignment.employee_id
        ) AS worker_row_no,
        COUNT(*) OVER (
            PARTITION BY
                preparation.tenant_id,
                preparation.target_date,
                assignment.customer_id,
                assignment.customer_site_id
        ) AS total_worker_count
    FROM daily_preparations preparation
    JOIN daily_preparation_assignments assignment
      ON assignment.tenant_id = preparation.tenant_id
     AND assignment.preparation_id = preparation.id
     AND assignment.deleted_at IS NULL
    LEFT JOIN daily_preparation_dispatches dispatch
      ON dispatch.tenant_id = preparation.tenant_id
     AND dispatch.preparation_id = preparation.id
     AND dispatch.customer_id <=> assignment.customer_id
     AND dispatch.customer_site_id <=> assignment.customer_site_id
     AND dispatch.deleted_at IS NULL
    LEFT JOIN active_company company
      ON company.tenant_id = preparation.tenant_id
     AND company.company_row_no = 1
    WHERE preparation.deleted_at IS NULL
)
SELECT
    ranked.*,
    FLOOR((ranked.worker_row_no - 1) / 10) + 1 AS page_no,
    CEIL(ranked.total_worker_count / 10) AS total_pages,
    MOD(ranked.worker_row_no - 1, 10) + 1 AS page_slot
FROM ranked_assignment ranked;

DROP PROCEDURE IF EXISTS sp_daily_work_order_prepare;
DELIMITER $$

CREATE PROCEDURE sp_daily_work_order_prepare(
    IN p_execution_id VARCHAR(100)
)
BEGIN
    DELETE FROM daily_work_order_render_output
    WHERE execution_id = p_execution_id;

    INSERT INTO daily_work_order_render_output (
        tenant_id, created_at, updated_at, execution_id, slip_key,
        target_date, weekday_label,
        customer_id, customer_site_id, customer_name, site_name,
        page_no, total_pages, page_worker_count,
        distance_from_company_km, vehicle_count,
        company_name, company_phone, company_fax,
        employee_name_1, employee_name_2, employee_name_3,
        employee_name_4, employee_name_5, employee_name_6,
        employee_name_7, employee_name_8, employee_name_9,
        employee_name_10,
        work_description_1, work_description_2, work_description_3,
        work_description_4, work_description_5, work_description_6,
        work_description_7, work_description_8, work_description_9,
        work_description_10
    )
    SELECT
        source.tenant_id,
        NOW(6),
        NOW(6),
        p_execution_id,
        CONCAT(
            'DAILY_WORK_ORDER:', DATE_FORMAT(source.target_date, '%Y-%m-%d'), ':',
            COALESCE(source.customer_id, 0), ':',
            COALESCE(source.customer_site_id, 0), ':',
            source.page_no
        ),
        source.target_date,
        ELT(DAYOFWEEK(source.target_date), '日', '月', '火', '水', '木', '金', '土'),
        source.customer_id,
        source.customer_site_id,
        MAX(source.customer_name),
        MAX(source.site_name),
        source.page_no,
        MAX(source.total_pages),
        COUNT(*),
        MAX(source.distance_from_company_km),
        MAX(source.vehicle_count),
        MAX(source.company_name),
        MAX(source.company_phone),
        MAX(source.company_fax),
        MAX(CASE WHEN source.page_slot = 1 THEN source.employee_name END),
        MAX(CASE WHEN source.page_slot = 2 THEN source.employee_name END),
        MAX(CASE WHEN source.page_slot = 3 THEN source.employee_name END),
        MAX(CASE WHEN source.page_slot = 4 THEN source.employee_name END),
        MAX(CASE WHEN source.page_slot = 5 THEN source.employee_name END),
        MAX(CASE WHEN source.page_slot = 6 THEN source.employee_name END),
        MAX(CASE WHEN source.page_slot = 7 THEN source.employee_name END),
        MAX(CASE WHEN source.page_slot = 8 THEN source.employee_name END),
        MAX(CASE WHEN source.page_slot = 9 THEN source.employee_name END),
        MAX(CASE WHEN source.page_slot = 10 THEN source.employee_name END),
        MAX(CASE WHEN source.page_slot = 1 THEN source.work_description END),
        MAX(CASE WHEN source.page_slot = 2 THEN source.work_description END),
        MAX(CASE WHEN source.page_slot = 3 THEN source.work_description END),
        MAX(CASE WHEN source.page_slot = 4 THEN source.work_description END),
        MAX(CASE WHEN source.page_slot = 5 THEN source.work_description END),
        MAX(CASE WHEN source.page_slot = 6 THEN source.work_description END),
        MAX(CASE WHEN source.page_slot = 7 THEN source.work_description END),
        MAX(CASE WHEN source.page_slot = 8 THEN source.work_description END),
        MAX(CASE WHEN source.page_slot = 9 THEN source.work_description END),
        MAX(CASE WHEN source.page_slot = 10 THEN source.work_description END)
    FROM vw_daily_work_order_render_source source
    JOIN daily_work_order_input input
      ON input.execution_id = p_execution_id
     AND input.tenant_id = source.tenant_id
     AND input.target_date = source.target_date
     AND input.deleted_at IS NULL
    GROUP BY
        source.tenant_id,
        source.target_date,
        source.customer_id,
        source.customer_site_id,
        source.page_no;
END$$

DELIMITER ;

DROP PROCEDURE IF EXISTS sp_daily_work_order_cleanup;
DELIMITER $$

CREATE PROCEDURE sp_daily_work_order_cleanup(
    IN p_execution_id VARCHAR(100)
)
BEGIN
    DELETE FROM daily_work_order_render_output
    WHERE execution_id = p_execution_id;

    DELETE FROM daily_work_order_input
    WHERE execution_id = p_execution_id;
END$$

DELIMITER ;

SET @tenant_id = 'default';
SET @now = NOW(6);

DELETE param
FROM report_param param
JOIN report_master master ON master.id = param.report_master_id
WHERE master.tenant_id = @tenant_id
  AND master.report_code = 'DAILY_WORK_ORDER';

DELETE FROM report_master
WHERE tenant_id = @tenant_id
  AND report_code = 'DAILY_WORK_ORDER';

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
    'DAILY_WORK_ORDER', '作業証明伝票', 'daily_work_order.jrxml',
    'daily_work_order', 'daily_work_order_input', 'daily_work_order_render_output',
    'vw_daily_work_order_render_source', NULL,
    'PROCEDURE', NULL, 'sp_daily_work_order_prepare',
    'SELECT * FROM daily_work_order_render_output WHERE execution_id = :executionId ORDER BY target_date, customer_name, site_name, page_no',
    'PROCEDURE', NULL, 'sp_daily_work_order_cleanup',
    'SINGLE', 1, '作業証明伝票_${targetDate}', 'PDF',
    FALSE, TRUE, TRUE
);

SET @report_master_id = (
    SELECT id FROM report_master
    WHERE tenant_id = @tenant_id
      AND report_code = 'DAILY_WORK_ORDER'
    LIMIT 1
);

INSERT INTO report_param (
    tenant_id, created_at, updated_at, report_master_id,
    param_name, param_label, param_type, control_type,
    required_flag, visible_flag, multiple_flag, filter_flag,
    default_value, placeholder, input_column_name,
    display_order, active_flag
) VALUES (
    @tenant_id, @now, @now, @report_master_id,
    'targetDate', '対象日', 'DATE', 'DATE',
    TRUE, TRUE, FALSE, TRUE,
    NULL, '対象日を選択', 'target_date', 1, TRUE
);

INSERT INTO batch_job_definition (
    tenant_id, job_code, job_name, job_type, target_code,
    immediate_executable, schedule_enabled, schedule_type,
    cron_expression, active_flag, description,
    created_at, updated_at
) VALUES (
    @tenant_id,
    'PRINT_DAILY_WORK_ORDER',
    '作業証明伝票出力',
    'REPORT',
    'DAILY_WORK_ORDER',
    TRUE, FALSE, 'NONE', NULL, TRUE,
    '翌日準備から顧客・現場ごとの作業証明伝票PDFを生成する',
    @now, @now
)
ON DUPLICATE KEY UPDATE
    job_name = VALUES(job_name),
    job_type = VALUES(job_type),
    target_code = VALUES(target_code),
    immediate_executable = VALUES(immediate_executable),
    schedule_enabled = VALUES(schedule_enabled),
    schedule_type = VALUES(schedule_type),
    active_flag = VALUES(active_flag),
    description = VALUES(description),
    updated_at = VALUES(updated_at);

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE operation_report_preview ADD COLUMN target_param_name VARCHAR(100) NULL AFTER filter_column_name',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_report_preview'
      AND column_name = 'target_param_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE operation_report_preview
    MODIFY COLUMN output_type VARCHAR(30) NOT NULL;

INSERT INTO operation_report_preview (
    tenant_id, created_at, updated_at,
    operation_type, report_code, report_name, job_code,
    table_name, filter_column_name, target_param_name,
    template_name, order_by, display_order,
    active_flag, output_type
) VALUES (
    @tenant_id, @now, @now,
    'PREPARATION', 'DAILY_WORK_ORDER', '作業証明伝票',
    'PRINT_DAILY_WORK_ORDER',
    'vw_daily_work_order_render_source', 'target_date', 'targetDate',
    'daily_work_order.jrxml',
    'customer_name, site_name, page_no',
    10, TRUE, 'PDF'
)
ON DUPLICATE KEY UPDATE
    report_name = VALUES(report_name),
    job_code = VALUES(job_code),
    table_name = VALUES(table_name),
    filter_column_name = VALUES(filter_column_name),
    target_param_name = VALUES(target_param_name),
    template_name = VALUES(template_name),
    order_by = VALUES(order_by),
    display_order = VALUES(display_order),
    active_flag = VALUES(active_flag),
    output_type = VALUES(output_type),
    updated_at = VALUES(updated_at);

SET @operation_preview_id = (
    SELECT id
    FROM operation_report_preview
    WHERE tenant_id = @tenant_id
      AND operation_type = 'PREPARATION'
      AND report_code = 'DAILY_WORK_ORDER'
    LIMIT 1
);

DELETE FROM operation_report_preview_column
WHERE operation_report_preview_id = @operation_preview_id;

INSERT INTO operation_report_preview_column (
    tenant_id, created_at, updated_at,
    operation_report_preview_id,
    preview_name, column_name, display_order, active_flag
) VALUES
(@tenant_id, @now, @now, @operation_preview_id, '対象日', 'target_date', 1, TRUE),
(@tenant_id, @now, @now, @operation_preview_id, '得意先', 'customer_name', 2, TRUE),
(@tenant_id, @now, @now, @operation_preview_id, '現場名', 'site_name', 3, TRUE),
(@tenant_id, @now, @now, @operation_preview_id, '作業員名', 'employee_name', 4, TRUE),
(@tenant_id, @now, @now, @operation_preview_id, '作業内容', 'work_description', 5, TRUE),
(@tenant_id, @now, @now, @operation_preview_id, '距離(km)', 'distance_from_company_km', 6, TRUE),
(@tenant_id, @now, @now, @operation_preview_id, '配車台数', 'vehicle_count', 7, TRUE);
