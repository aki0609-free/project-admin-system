-- ProjectAdmin 月次請求書 パターン別描画View V1
-- monthly_invoice_foundation_v1.sqlの後に適用する。

SET NAMES utf8mb4;

CREATE OR REPLACE VIEW vw_monthly_invoice_pattern_1_render AS
SELECT
    flat.execution_id,
    flat.id AS invoice_history_id,
    flat.invoice_number,
    flat.issue_date,
    flat.target_month,
    flat.period_from,
    flat.period_to,
    flat.closing_version,
    flat.customer_id,
    flat.customer_name,
    flat.company_name,
    flat.company_postal_code,
    flat.company_address,
    flat.company_phone,
    flat.company_fax,
    flat.qualified_invoice_issuer_number,
    flat.bank_display_text,
    MAX(flat.company_logo_image_data) AS company_logo_image_data,
    flat.invoice_note,
    flat.tax_rate,
    flat.subtotal_amount,
    flat.tax_amount,
    flat.total_amount,
    flat.work_date,
    flat.job_code,
    flat.job_name,
    SUM(flat.base_quantity) AS base_quantity,
    MAX(flat.base_unit_price) AS base_unit_price,
    SUM(flat.base_amount) AS base_amount,
    SUM(flat.overtime_hours) AS overtime_hours,
    MAX(flat.overtime_unit_price) AS overtime_unit_price,
    SUM(flat.overtime_amount) AS overtime_amount,
    SUM(flat.night_hours) AS night_hours,
    MAX(flat.night_unit_price) AS night_unit_price,
    SUM(flat.night_amount) AS night_amount,
    SUM(flat.holiday_hours) AS holiday_hours,
    MAX(flat.holiday_unit_price) AS holiday_unit_price,
    SUM(flat.holiday_amount) AS holiday_amount,
    SUM(flat.commute_distance) AS commute_distance,
    MAX(flat.commute_unit_price) AS commute_unit_price,
    SUM(flat.commute_amount) AS commute_amount,
    SUM(flat.holiday_amount) + SUM(flat.other_amount) AS other_amount,
    SUM(flat.line_amount) AS line_amount
FROM vw_monthly_invoice_render_flat flat
GROUP BY
    flat.execution_id, flat.id, flat.invoice_number, flat.issue_date,
    flat.target_month, flat.period_from, flat.period_to,
    flat.closing_version, flat.customer_id, flat.customer_name,
    flat.company_name, flat.company_postal_code, flat.company_address,
    flat.company_phone, flat.company_fax,
    flat.qualified_invoice_issuer_number, flat.bank_display_text,
    flat.invoice_note, flat.tax_rate, flat.subtotal_amount,
    flat.tax_amount, flat.total_amount, flat.work_date,
    flat.job_code, flat.job_name;

CREATE OR REPLACE VIEW vw_monthly_invoice_metric_source AS
SELECT flat.*, 1 AS metric_order, '通常' AS metric_name,
       flat.base_quantity AS quantity,
       flat.base_unit_price AS unit_price,
       flat.base_amount AS amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 2, '残業', flat.overtime_hours,
       flat.overtime_unit_price, flat.overtime_amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 3, '深夜', flat.night_hours,
       flat.night_unit_price, flat.night_amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 4, '休日', flat.holiday_hours,
       flat.holiday_unit_price, flat.holiday_amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 5, '通勤', flat.commute_distance,
       flat.commute_unit_price, flat.commute_amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 6, 'その他金額',
       CASE WHEN flat.other_amount <> 0 THEN 1 ELSE 0 END,
       flat.other_amount, flat.other_amount
FROM vw_monthly_invoice_render_flat flat;

-- パターン2・3は31日マトリクス内の単位を帳票仕様に合わせる。
CREATE OR REPLACE VIEW vw_monthly_invoice_pattern_2_metric_source AS
SELECT flat.*, 1 AS metric_order, '通常（人）' AS metric_name,
       flat.base_quantity AS quantity,
       flat.base_unit_price AS unit_price,
       flat.base_amount AS amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 2, '残業（時間）', flat.overtime_hours,
       flat.overtime_unit_price, flat.overtime_amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 3, '深夜（時間）', flat.night_hours,
       flat.night_unit_price, flat.night_amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 4, '通勤（円）', flat.commute_amount,
       NULL, flat.commute_amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 5, 'その他（円）', flat.holiday_amount + flat.other_amount,
       NULL, flat.holiday_amount + flat.other_amount
FROM vw_monthly_invoice_render_flat flat;

CREATE OR REPLACE VIEW vw_monthly_invoice_pattern_3_metric_source AS
SELECT flat.*, 1 AS metric_order, '通常（人）' AS metric_name,
       flat.base_quantity AS quantity,
       flat.base_unit_price AS unit_price,
       flat.base_amount AS amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 2, '残業（時間）', flat.overtime_hours,
       flat.overtime_unit_price, flat.overtime_amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 3, '深夜（時間）', flat.night_hours,
       flat.night_unit_price, flat.night_amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 4, '通勤（円）', flat.commute_amount,
       NULL, flat.commute_amount
FROM vw_monthly_invoice_render_flat flat
UNION ALL
SELECT flat.*, 5, 'その他（円）', flat.holiday_amount + flat.other_amount,
       NULL, flat.holiday_amount + flat.other_amount
FROM vw_monthly_invoice_render_flat flat;

CREATE OR REPLACE VIEW vw_monthly_invoice_pattern_2_render AS
SELECT
    metric.execution_id,
    metric.id AS invoice_history_id,
    metric.invoice_number,
    metric.issue_date,
    metric.target_month,
    metric.period_from,
    metric.period_to,
    metric.closing_version,
    metric.customer_id,
    metric.customer_name,
    metric.company_name,
    metric.company_postal_code,
    metric.company_address,
    metric.company_phone,
    metric.company_fax,
    metric.qualified_invoice_issuer_number,
    metric.bank_display_text,
    MAX(metric.company_logo_image_data) AS company_logo_image_data,
    metric.invoice_note,
    metric.tax_rate,
    metric.subtotal_amount,
    metric.tax_amount,
    metric.total_amount,
    metric.job_code,
    metric.job_name,
    metric.metric_order,
    metric.metric_name,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 0 THEN metric.quantity ELSE 0 END) AS day_01,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 1 THEN metric.quantity ELSE 0 END) AS day_02,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 2 THEN metric.quantity ELSE 0 END) AS day_03,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 3 THEN metric.quantity ELSE 0 END) AS day_04,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 4 THEN metric.quantity ELSE 0 END) AS day_05,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 5 THEN metric.quantity ELSE 0 END) AS day_06,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 6 THEN metric.quantity ELSE 0 END) AS day_07,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 7 THEN metric.quantity ELSE 0 END) AS day_08,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 8 THEN metric.quantity ELSE 0 END) AS day_09,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 9 THEN metric.quantity ELSE 0 END) AS day_10,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 10 THEN metric.quantity ELSE 0 END) AS day_11,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 11 THEN metric.quantity ELSE 0 END) AS day_12,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 12 THEN metric.quantity ELSE 0 END) AS day_13,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 13 THEN metric.quantity ELSE 0 END) AS day_14,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 14 THEN metric.quantity ELSE 0 END) AS day_15,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 15 THEN metric.quantity ELSE 0 END) AS day_16,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 16 THEN metric.quantity ELSE 0 END) AS day_17,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 17 THEN metric.quantity ELSE 0 END) AS day_18,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 18 THEN metric.quantity ELSE 0 END) AS day_19,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 19 THEN metric.quantity ELSE 0 END) AS day_20,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 20 THEN metric.quantity ELSE 0 END) AS day_21,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 21 THEN metric.quantity ELSE 0 END) AS day_22,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 22 THEN metric.quantity ELSE 0 END) AS day_23,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 23 THEN metric.quantity ELSE 0 END) AS day_24,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 24 THEN metric.quantity ELSE 0 END) AS day_25,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 25 THEN metric.quantity ELSE 0 END) AS day_26,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 26 THEN metric.quantity ELSE 0 END) AS day_27,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 27 THEN metric.quantity ELSE 0 END) AS day_28,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 28 THEN metric.quantity ELSE 0 END) AS day_29,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 29 THEN metric.quantity ELSE 0 END) AS day_30,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 30 THEN metric.quantity ELSE 0 END) AS day_31,
    SUM(metric.quantity) AS metric_total,
    MAX(metric.unit_price) AS unit_price,
    SUM(metric.amount) AS metric_amount,
    (
        SELECT SUM(job_flat.line_amount)
        FROM vw_monthly_invoice_render_flat job_flat
        WHERE job_flat.execution_id = metric.execution_id
          AND job_flat.job_code = metric.job_code
    ) AS job_total_amount
FROM vw_monthly_invoice_pattern_2_metric_source metric
GROUP BY
    metric.execution_id, metric.id, metric.invoice_number,
    metric.issue_date, metric.target_month, metric.period_from,
    metric.period_to, metric.closing_version,
    metric.customer_id, metric.customer_name,
    metric.company_name, metric.company_postal_code,
    metric.company_address, metric.company_phone, metric.company_fax,
    metric.qualified_invoice_issuer_number, metric.bank_display_text,
    metric.invoice_note, metric.tax_rate, metric.subtotal_amount,
    metric.tax_amount, metric.total_amount, metric.job_code,
    metric.job_name,
    metric.metric_order, metric.metric_name;
CREATE OR REPLACE VIEW vw_monthly_invoice_pattern_3_render AS
SELECT
    metric.execution_id,
    metric.id AS invoice_history_id,
    metric.invoice_number,
    metric.issue_date,
    metric.target_month,
    metric.period_from,
    metric.period_to,
    metric.closing_version,
    metric.customer_id,
    metric.customer_name,
    metric.company_name,
    metric.company_postal_code,
    metric.company_address,
    metric.company_phone,
    metric.company_fax,
    metric.qualified_invoice_issuer_number,
    metric.bank_display_text,
    MAX(metric.company_logo_image_data) AS company_logo_image_data,
    metric.invoice_note,
    metric.tax_rate,
    metric.subtotal_amount,
    metric.tax_amount,
    metric.total_amount,
    metric.customer_site_id,
    metric.site_name,
    metric.job_code,
    metric.job_name,
    metric.site_role_code,
    metric.site_role_name,
    CONCAT(
        COALESCE(metric.site_name, ''),
        CASE
            WHEN NULLIF(TRIM(metric.job_name), '') IS NULL THEN ''
            ELSE CONCAT(
                '（', metric.job_name,
                CASE
                    WHEN NULLIF(TRIM(metric.site_role_name), '') IS NULL THEN ''
                    ELSE CONCAT('／', metric.site_role_name)
                END,
                '）'
            )
        END
    ) AS aggregation_label,
    metric.metric_order,
    metric.metric_name,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 0 THEN metric.quantity ELSE 0 END) AS day_01,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 1 THEN metric.quantity ELSE 0 END) AS day_02,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 2 THEN metric.quantity ELSE 0 END) AS day_03,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 3 THEN metric.quantity ELSE 0 END) AS day_04,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 4 THEN metric.quantity ELSE 0 END) AS day_05,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 5 THEN metric.quantity ELSE 0 END) AS day_06,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 6 THEN metric.quantity ELSE 0 END) AS day_07,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 7 THEN metric.quantity ELSE 0 END) AS day_08,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 8 THEN metric.quantity ELSE 0 END) AS day_09,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 9 THEN metric.quantity ELSE 0 END) AS day_10,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 10 THEN metric.quantity ELSE 0 END) AS day_11,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 11 THEN metric.quantity ELSE 0 END) AS day_12,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 12 THEN metric.quantity ELSE 0 END) AS day_13,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 13 THEN metric.quantity ELSE 0 END) AS day_14,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 14 THEN metric.quantity ELSE 0 END) AS day_15,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 15 THEN metric.quantity ELSE 0 END) AS day_16,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 16 THEN metric.quantity ELSE 0 END) AS day_17,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 17 THEN metric.quantity ELSE 0 END) AS day_18,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 18 THEN metric.quantity ELSE 0 END) AS day_19,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 19 THEN metric.quantity ELSE 0 END) AS day_20,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 20 THEN metric.quantity ELSE 0 END) AS day_21,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 21 THEN metric.quantity ELSE 0 END) AS day_22,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 22 THEN metric.quantity ELSE 0 END) AS day_23,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 23 THEN metric.quantity ELSE 0 END) AS day_24,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 24 THEN metric.quantity ELSE 0 END) AS day_25,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 25 THEN metric.quantity ELSE 0 END) AS day_26,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 26 THEN metric.quantity ELSE 0 END) AS day_27,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 27 THEN metric.quantity ELSE 0 END) AS day_28,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 28 THEN metric.quantity ELSE 0 END) AS day_29,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 29 THEN metric.quantity ELSE 0 END) AS day_30,
    SUM(CASE WHEN DATEDIFF(metric.work_date, metric.period_from) = 30 THEN metric.quantity ELSE 0 END) AS day_31,
    SUM(metric.quantity) AS metric_total,
    MAX(metric.unit_price) AS unit_price,
    SUM(metric.amount) AS metric_amount,
    (
        SELECT SUM(combo_flat.line_amount)
        FROM vw_monthly_invoice_render_flat combo_flat
        WHERE combo_flat.execution_id = metric.execution_id
          AND combo_flat.customer_site_id <=> metric.customer_site_id
          AND combo_flat.job_code <=> metric.job_code
          AND combo_flat.site_role_code <=> metric.site_role_code
    ) AS job_total_amount
FROM vw_monthly_invoice_pattern_3_metric_source metric
GROUP BY
    metric.execution_id, metric.id, metric.invoice_number,
    metric.issue_date, metric.target_month, metric.period_from,
    metric.period_to, metric.closing_version,
    metric.customer_id, metric.customer_name,
    metric.company_name, metric.company_postal_code,
    metric.company_address, metric.company_phone, metric.company_fax,
    metric.qualified_invoice_issuer_number, metric.bank_display_text,
    metric.invoice_note, metric.tax_rate, metric.subtotal_amount,
    metric.tax_amount, metric.total_amount, metric.customer_site_id,
    metric.site_name, metric.job_code, metric.job_name,
    metric.site_role_code, metric.site_role_name,
    metric.metric_order, metric.metric_name;
