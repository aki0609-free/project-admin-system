-- ProjectAdmin 月次請求書 View・履歴・描画基盤 V1
-- MySQL 8.x

SET NAMES utf8mb4;

-- V1では期間固定額をMONTHLYへ統合する。
-- 既存データが残っていても、基盤適用時に安全に移行できるよう冪等なUPDATEとする。
UPDATE customer_site_billing_rates
SET billing_unit = 'MONTHLY'
WHERE billing_unit = 'FIXED';

UPDATE daily_report
SET billing_unit = 'MONTHLY'
WHERE billing_unit = 'FIXED';

CREATE TABLE IF NOT EXISTS monthly_invoice_input (
    id BIGINT NOT NULL AUTO_INCREMENT,
    execution_id VARCHAR(100) NOT NULL,
    target_month VARCHAR(7) NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    closing_version INT NOT NULL,
    execution_mode VARCHAR(30) NOT NULL,
    customer_id BIGINT NOT NULL,
    tax_rate DECIMAL(7,4) NOT NULL DEFAULT 0.1000,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_invoice_input_execution (tenant_id, execution_id),
    KEY idx_monthly_invoice_input_target
        (tenant_id, target_month, closing_version, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_invoice_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_month DATE NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    closing_version INT NOT NULL,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    invoice_type VARCHAR(30) NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    issue_date DATE NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    company_postal_code VARCHAR(20) NULL,
    company_address VARCHAR(1000) NULL,
    company_phone VARCHAR(50) NULL,
    company_fax VARCHAR(50) NULL,
    qualified_invoice_issuer_number VARCHAR(50) NULL,
    bank_display_text VARCHAR(1000) NULL,
    invoice_note TEXT NULL,
    tax_rate DECIMAL(7,4) NOT NULL,
    subtotal_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    business_key VARCHAR(255) NOT NULL,
    source_execution_id VARCHAR(100) NOT NULL,
    fixed_at TIMESTAMP(6) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_invoice_history_version
        (tenant_id, target_month, closing_version, customer_id),
    UNIQUE KEY uk_monthly_invoice_history_business
        (tenant_id, business_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_invoice_history_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    monthly_invoice_history_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    work_date DATE NOT NULL,
    customer_site_id BIGINT NULL,
    site_name VARCHAR(255) NULL,
    job_code VARCHAR(100) NULL,
    job_name VARCHAR(200) NULL,
    site_role_code VARCHAR(100) NULL,
    site_role_name VARCHAR(200) NULL,
    billing_unit VARCHAR(30) NOT NULL,
    base_quantity DECIMAL(12,4) NOT NULL DEFAULT 0,
    base_unit_price DECIMAL(15,2) NOT NULL DEFAULT 0,
    base_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    overtime_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    overtime_unit_price DECIMAL(15,2) NOT NULL DEFAULT 0,
    overtime_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    night_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    night_unit_price DECIMAL(15,2) NOT NULL DEFAULT 0,
    night_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    holiday_hours DECIMAL(10,2) NOT NULL DEFAULT 0,
    holiday_unit_price DECIMAL(15,2) NOT NULL DEFAULT 0,
    holiday_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    commute_distance DECIMAL(10,2) NOT NULL DEFAULT 0,
    commute_unit_price DECIMAL(15,2) NOT NULL DEFAULT 0,
    commute_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    line_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_invoice_history_detail
        (tenant_id, monthly_invoice_history_id, line_no),
    CONSTRAINT fk_monthly_invoice_history_detail_header
        FOREIGN KEY (monthly_invoice_history_id)
        REFERENCES monthly_invoice_history (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_invoice_render_execution (
    id BIGINT NOT NULL AUTO_INCREMENT,
    execution_id VARCHAR(100) NOT NULL,
    monthly_invoice_history_id BIGINT NOT NULL,
    recipient_key VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    business_key VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_monthly_invoice_render_execution
        (tenant_id, execution_id, monthly_invoice_history_id),
    CONSTRAINT fk_monthly_invoice_render_execution_history
        FOREIGN KEY (monthly_invoice_history_id)
        REFERENCES monthly_invoice_history (id),
    KEY idx_monthly_invoice_render_business
        (tenant_id, execution_id, business_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 顧客締め実行の期間内で、MONTHLYの基本料金は同じ請求単価につき一度だけ計上する。
-- 残業・深夜・休日・通勤費は日報ごとの実績として従来どおり加算する。
CREATE OR REPLACE VIEW vw_monthly_invoice_execution_detail AS
SELECT
    source.execution_id,
    source.tenant_id,
    source.customer_id,
    source.work_date,
    source.customer_site_id,
    source.site_name,
    source.job_code,
    source.job_name,
    source.site_role_code,
    source.site_role_name,
    source.billing_unit,
    CASE
        WHEN source.billing_unit = 'DAILY'
            THEN source.work_hours / 8
        WHEN source.billing_unit = 'HOURLY'
            THEN source.work_hours
        WHEN source.billing_unit = 'MONTHLY'
             AND source.billing_rate_occurrence = 1
            THEN 1
        ELSE 0
    END AS base_quantity,
    source.billing_base_unit_price AS base_unit_price,
    ROUND(
        CASE
            WHEN source.billing_unit = 'DAILY'
                THEN source.work_hours / 8 * source.billing_base_unit_price
            WHEN source.billing_unit = 'HOURLY'
                THEN source.work_hours * source.billing_base_unit_price
            WHEN source.billing_unit = 'MONTHLY'
                 AND source.billing_rate_occurrence = 1
                THEN source.billing_base_unit_price
            ELSE 0
        END,
        0
    ) AS base_amount,
    source.overtime_hours,
    source.billing_overtime_unit_price AS overtime_unit_price,
    ROUND(
        source.overtime_hours * source.billing_overtime_unit_price,
        0
    ) AS overtime_amount,
    source.night_work_hours AS night_hours,
    source.billing_night_unit_price AS night_unit_price,
    ROUND(
        source.night_work_hours * source.billing_night_unit_price,
        0
    ) AS night_amount,
    source.holiday_work_hours AS holiday_hours,
    source.billing_holiday_unit_price AS holiday_unit_price,
    ROUND(
        source.holiday_work_hours * source.billing_holiday_unit_price,
        0
    ) AS holiday_amount,
    source.mileage AS commute_distance,
    source.billing_commute_unit_price AS commute_unit_price,
    ROUND(
        source.mileage * source.billing_commute_unit_price,
        0
    ) AS commute_amount,
    source.billing_unit IN ('DAILY', 'HOURLY', 'MONTHLY')
        AND (source.billing_unit <> 'MONTHLY' OR source.billing_rate_id IS NOT NULL)
        AS calculation_ready_flag
FROM (
    SELECT
        input.execution_id,
        dr.tenant_id,
        dr.customer_id,
        dr.work_date,
        dr.customer_site_id,
        COALESCE(dr.site_name, site.name, '') AS site_name,
        dr.job_code,
        dr.job_name,
        dr.site_role_code,
        dr.site_role_name,
        dr.billing_rate_id,
        dr.billing_unit,
        COALESCE(dr.work_hours, 0) AS work_hours,
        COALESCE(dr.billing_base_unit_price, 0) AS billing_base_unit_price,
        COALESCE(dr.overtime_hours, 0) AS overtime_hours,
        COALESCE(dr.billing_overtime_unit_price, 0) AS billing_overtime_unit_price,
        COALESCE(dr.night_work_hours, 0) AS night_work_hours,
        COALESCE(dr.billing_night_unit_price, 0) AS billing_night_unit_price,
        COALESCE(dr.holiday_work_hours, 0) AS holiday_work_hours,
        COALESCE(dr.billing_holiday_unit_price, 0) AS billing_holiday_unit_price,
        COALESCE(dr.mileage, 0) AS mileage,
        COALESCE(dr.billing_commute_unit_price, 0) AS billing_commute_unit_price,
        ROW_NUMBER() OVER (
            PARTITION BY input.execution_id, dr.billing_rate_id
            ORDER BY dr.work_date, dr.id
        ) AS billing_rate_occurrence
    FROM monthly_invoice_input input
    JOIN daily_report dr
      ON dr.tenant_id = input.tenant_id
     AND dr.customer_id = input.customer_id
     AND dr.work_date BETWEEN input.period_from AND input.period_to
     AND dr.deleted_at IS NULL
     AND dr.approval_status = 'APPROVED'
    LEFT JOIN customer_sites site
      ON site.tenant_id = dr.tenant_id
     AND site.id = dr.customer_site_id
     AND site.deleted_at IS NULL
    WHERE input.deleted_at IS NULL
) source;

-- 月選択プレビュー／対象顧客検索向けの互換View。
-- 本締めは必ず上のexecution Viewを使い、顧客固有の締め期間で正確に確定する。
CREATE OR REPLACE VIEW vw_monthly_invoice_latest_detail AS
SELECT
    source.tenant_id,
    source.customer_id,
    source.work_date,
    source.customer_site_id,
    source.site_name,
    source.job_code,
    source.job_name,
    source.site_role_code,
    source.site_role_name,
    source.billing_unit,
    CASE
        WHEN source.billing_unit = 'DAILY' THEN source.work_hours / 8
        WHEN source.billing_unit = 'HOURLY' THEN source.work_hours
        WHEN source.billing_unit = 'MONTHLY'
             AND source.billing_rate_occurrence = 1 THEN 1
        ELSE 0
    END AS base_quantity,
    source.billing_base_unit_price AS base_unit_price,
    ROUND(
        CASE
            WHEN source.billing_unit = 'DAILY'
                THEN source.work_hours / 8 * source.billing_base_unit_price
            WHEN source.billing_unit = 'HOURLY'
                THEN source.work_hours * source.billing_base_unit_price
            WHEN source.billing_unit = 'MONTHLY'
                 AND source.billing_rate_occurrence = 1
                THEN source.billing_base_unit_price
            ELSE 0
        END,
        0
    ) AS base_amount,
    source.overtime_hours,
    source.billing_overtime_unit_price AS overtime_unit_price,
    ROUND(source.overtime_hours * source.billing_overtime_unit_price, 0)
        AS overtime_amount,
    source.night_work_hours AS night_hours,
    source.billing_night_unit_price AS night_unit_price,
    ROUND(source.night_work_hours * source.billing_night_unit_price, 0)
        AS night_amount,
    source.holiday_work_hours AS holiday_hours,
    source.billing_holiday_unit_price AS holiday_unit_price,
    ROUND(source.holiday_work_hours * source.billing_holiday_unit_price, 0)
        AS holiday_amount,
    source.mileage AS commute_distance,
    source.billing_commute_unit_price AS commute_unit_price,
    ROUND(source.mileage * source.billing_commute_unit_price, 0)
        AS commute_amount,
    source.billing_unit IN ('DAILY', 'HOURLY', 'MONTHLY')
        AND (source.billing_unit <> 'MONTHLY' OR source.billing_rate_id IS NOT NULL)
        AS calculation_ready_flag
FROM (
    SELECT
        dr.tenant_id,
        dr.customer_id,
        dr.work_date,
        dr.customer_site_id,
        COALESCE(dr.site_name, site.name, '') AS site_name,
        dr.job_code,
        dr.job_name,
        dr.site_role_code,
        dr.site_role_name,
        dr.billing_rate_id,
        dr.billing_unit,
        COALESCE(dr.work_hours, 0) AS work_hours,
        COALESCE(dr.billing_base_unit_price, 0) AS billing_base_unit_price,
        COALESCE(dr.overtime_hours, 0) AS overtime_hours,
        COALESCE(dr.billing_overtime_unit_price, 0) AS billing_overtime_unit_price,
        COALESCE(dr.night_work_hours, 0) AS night_work_hours,
        COALESCE(dr.billing_night_unit_price, 0) AS billing_night_unit_price,
        COALESCE(dr.holiday_work_hours, 0) AS holiday_work_hours,
        COALESCE(dr.billing_holiday_unit_price, 0) AS billing_holiday_unit_price,
        COALESCE(dr.mileage, 0) AS mileage,
        COALESCE(dr.billing_commute_unit_price, 0) AS billing_commute_unit_price,
        ROW_NUMBER() OVER (
            PARTITION BY
                dr.tenant_id,
                dr.customer_id,
                DATE_FORMAT(dr.work_date, '%Y-%m'),
                dr.billing_rate_id
            ORDER BY dr.work_date, dr.id
        ) AS billing_rate_occurrence
    FROM daily_report dr
    LEFT JOIN customer_sites site
      ON site.tenant_id = dr.tenant_id
     AND site.id = dr.customer_site_id
     AND site.deleted_at IS NULL
    WHERE dr.deleted_at IS NULL
      AND dr.approval_status = 'APPROVED'
      AND dr.customer_id IS NOT NULL
) source;

CREATE OR REPLACE VIEW vw_monthly_invoice_render_flat AS
SELECT
    render.execution_id,
    render.recipient_key,
    render.recipient_name,
    history.*,
    detail.line_no,
    detail.work_date,
    detail.customer_site_id,
    detail.site_name,
    detail.job_code,
    detail.job_name,
    detail.site_role_code,
    detail.site_role_name,
    detail.billing_unit,
    detail.base_quantity,
    detail.base_unit_price,
    detail.base_amount,
    detail.overtime_hours,
    detail.overtime_unit_price,
    detail.overtime_amount,
    detail.night_hours,
    detail.night_unit_price,
    detail.night_amount,
    detail.holiday_hours,
    detail.holiday_unit_price,
    detail.holiday_amount,
    detail.commute_distance,
    detail.commute_unit_price,
    detail.commute_amount,
    detail.line_amount
FROM monthly_invoice_render_execution render
JOIN monthly_invoice_history history
  ON history.id = render.monthly_invoice_history_id
 AND history.tenant_id = render.tenant_id
 AND history.deleted_at IS NULL
LEFT JOIN monthly_invoice_history_detail detail
  ON detail.monthly_invoice_history_id = history.id
 AND detail.tenant_id = history.tenant_id
 AND detail.deleted_at IS NULL
WHERE render.deleted_at IS NULL;

DROP PROCEDURE IF EXISTS sp_monthly_invoice_snapshot;

DELIMITER $$

CREATE PROCEDURE sp_monthly_invoice_snapshot(IN p_execution_id VARCHAR(100))
BEGIN
    DECLARE v_tenant_id VARCHAR(255);
    DECLARE v_target_month DATE;
    DECLARE v_period_from DATE;
    DECLARE v_period_to DATE;
    DECLARE v_closing_version INT;
    DECLARE v_execution_mode VARCHAR(30);
    DECLARE v_customer_id BIGINT;
    DECLARE v_tax_rate DECIMAL(7,4);
    DECLARE v_history_id BIGINT;
    DECLARE v_source_count BIGINT DEFAULT 0;
    DECLARE v_not_ready_count BIGINT DEFAULT 0;
    DECLARE v_inserted_count INT DEFAULT 0;

    SELECT
        input.tenant_id,
        STR_TO_DATE(CONCAT(input.target_month, '-01'), '%Y-%m-%d'),
        input.period_from,
        input.period_to,
        input.closing_version,
        UPPER(input.execution_mode),
        input.customer_id,
        input.tax_rate
    INTO
        v_tenant_id,
        v_target_month,
        v_period_from,
        v_period_to,
        v_closing_version,
        v_execution_mode,
        v_customer_id,
        v_tax_rate
    FROM monthly_invoice_input input
    WHERE input.execution_id = p_execution_id
      AND input.deleted_at IS NULL
    ORDER BY input.id DESC
    LIMIT 1;

    IF v_tenant_id IS NULL OR v_customer_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'monthly invoice input is missing';
    END IF;

    IF v_execution_mode NOT IN ('INITIAL', 'RECLOSE', 'RETRY') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'invalid monthly invoice execution mode';
    END IF;

    IF v_execution_mode IN ('INITIAL', 'RECLOSE') THEN
        SELECT COUNT(*)
        INTO v_source_count
        FROM vw_monthly_invoice_execution_detail source
        WHERE source.execution_id = p_execution_id;

        IF v_source_count = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'monthly invoice source data is missing';
        END IF;

        SELECT COUNT(*)
        INTO v_not_ready_count
        FROM vw_monthly_invoice_execution_detail source
        WHERE source.execution_id = p_execution_id
          AND source.calculation_ready_flag = FALSE;

        IF v_not_ready_count > 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'unsupported or incomplete billing unit configuration';
        END IF;

        INSERT INTO monthly_invoice_history (
            target_month, period_from, period_to, closing_version,
            customer_id, customer_name, invoice_type,
            invoice_number, issue_date,
            company_name, company_postal_code, company_address,
            company_phone, company_fax,
            qualified_invoice_issuer_number,
            bank_display_text, invoice_note,
            tax_rate, subtotal_amount, tax_amount, total_amount,
            business_key, source_execution_id, fixed_at,
            tenant_id, created_at, updated_at
        )
        SELECT
            v_target_month,
            v_period_from,
            v_period_to,
            v_closing_version,
            customer.id,
            customer.name,
            COALESCE(customer.invoice_type, 'PATTERN_1'),
            CONCAT(
                DATE_FORMAT(v_target_month, '%Y%m'), '-',
                LPAD(customer.id, 6, '0'), '-V', v_closing_version
            ),
            v_period_to,
            company.company_name,
            company.postal_code,
            CONCAT_WS(
                '',
                company.prefecture,
                company.city,
                company.address_line1,
                company.address_line2
            ),
            company.phone,
            company.fax,
            company.qualified_invoice_issuer_number,
            CONCAT_WS(
                ' ',
                company.invoice_bank_name,
                company.invoice_bank_branch_name,
                company.invoice_bank_account_type,
                company.invoice_bank_account_number,
                company.invoice_bank_account_holder
            ),
            company.invoice_note,
            v_tax_rate,
            0, 0, 0,
            CONCAT(
                'MONTHLY_INVOICE:',
                DATE_FORMAT(v_target_month, '%Y-%m'), ':',
                customer.id, ':V', v_closing_version
            ),
            p_execution_id,
            NOW(6),
            v_tenant_id,
            NOW(6),
            NOW(6)
        FROM customers customer
        JOIN company_profile company
          ON company.tenant_id = customer.tenant_id
         AND company.active_flag = TRUE
         AND company.deleted_at IS NULL
        WHERE customer.tenant_id = v_tenant_id
          AND customer.id = v_customer_id
          AND customer.deleted_at IS NULL
        ORDER BY company.id
        LIMIT 1;

        SET v_inserted_count = ROW_COUNT();

        IF v_inserted_count <> 1 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'customer or active company profile is missing';
        END IF;

        SET v_history_id = LAST_INSERT_ID();

        INSERT INTO monthly_invoice_history_detail (
            monthly_invoice_history_id, line_no, work_date,
            customer_site_id, site_name, job_code, job_name,
            site_role_code, site_role_name, billing_unit,
            base_quantity, base_unit_price, base_amount,
            overtime_hours, overtime_unit_price, overtime_amount,
            night_hours, night_unit_price, night_amount,
            holiday_hours, holiday_unit_price, holiday_amount,
            commute_distance, commute_unit_price, commute_amount,
            line_amount,
            tenant_id, created_at, updated_at
        )
        SELECT
            v_history_id,
            ROW_NUMBER() OVER (
                ORDER BY
                    source.work_date,
                    source.site_name,
                    source.job_code,
                    source.site_role_code
            ),
            source.work_date,
            source.customer_site_id,
            source.site_name,
            source.job_code,
            source.job_name,
            source.site_role_code,
            source.site_role_name,
            source.billing_unit,
            source.base_quantity,
            source.base_unit_price,
            source.base_amount,
            source.overtime_hours,
            source.overtime_unit_price,
            source.overtime_amount,
            source.night_hours,
            source.night_unit_price,
            source.night_amount,
            source.holiday_hours,
            source.holiday_unit_price,
            source.holiday_amount,
            source.commute_distance,
            source.commute_unit_price,
            source.commute_amount,
            source.base_amount
                + source.overtime_amount
                + source.night_amount
                + source.holiday_amount
                + source.commute_amount,
            v_tenant_id,
            NOW(6),
            NOW(6)
        FROM vw_monthly_invoice_execution_detail source
        WHERE source.execution_id = p_execution_id;

        UPDATE monthly_invoice_history history
        SET
            history.subtotal_amount = (
                SELECT COALESCE(SUM(detail.line_amount), 0)
                FROM monthly_invoice_history_detail detail
                WHERE detail.monthly_invoice_history_id = v_history_id
                  AND detail.deleted_at IS NULL
            ),
            history.tax_amount = ROUND(
                (
                    SELECT COALESCE(SUM(detail.line_amount), 0)
                    FROM monthly_invoice_history_detail detail
                    WHERE detail.monthly_invoice_history_id = v_history_id
                      AND detail.deleted_at IS NULL
                ) * history.tax_rate,
                0
            ),
            history.total_amount = history.subtotal_amount + history.tax_amount,
            history.updated_at = NOW(6)
        WHERE history.id = v_history_id;
    ELSE
        SELECT history.id
        INTO v_history_id
        FROM monthly_invoice_history history
        WHERE history.tenant_id = v_tenant_id
          AND history.target_month = v_target_month
          AND history.closing_version = v_closing_version
          AND history.customer_id = v_customer_id
          AND history.deleted_at IS NULL
        LIMIT 1;

        IF v_history_id IS NULL THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'monthly invoice history is missing for RETRY';
        END IF;
    END IF;

    DELETE FROM monthly_invoice_render_execution
    WHERE execution_id = p_execution_id;

    INSERT INTO monthly_invoice_render_execution (
        execution_id, monthly_invoice_history_id,
        recipient_key, recipient_name, business_key,
        tenant_id, created_at, updated_at
    )
    SELECT
        p_execution_id,
        history.id,
        CAST(history.customer_id AS CHAR),
        history.customer_name,
        history.business_key,
        history.tenant_id,
        NOW(6),
        NOW(6)
    FROM monthly_invoice_history history
    WHERE history.id = v_history_id;
END$$

DELIMITER ;

DROP PROCEDURE IF EXISTS sp_monthly_invoice_cleanup;

DELIMITER $$

CREATE PROCEDURE sp_monthly_invoice_cleanup(IN p_execution_id VARCHAR(100))
BEGIN
    DELETE FROM monthly_invoice_render_execution
    WHERE execution_id = p_execution_id;

    DELETE FROM monthly_invoice_input
    WHERE execution_id = p_execution_id;
END$$

DELIMITER ;
