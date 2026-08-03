-- ProjectAdminSystem V1
-- 月間集計表（第7期フォーマット）用DDL／マスターデータ
--
-- 実行順:
--   1. layout_type追加
--   2. 台帳専用View作成
--   3. データソースカタログ登録
--
-- 将来、別レイアウトを追加する場合はlayout_typeとRendererを追加し、
-- このViewへ画面レイアウト固有の列を持たせない。

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE excel_book_master ADD COLUMN layout_type VARCHAR(30) NOT NULL DEFAULT ''REPEATING_ROW'' AFTER source_type',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'excel_book_master'
      AND column_name = 'layout_type'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW vw_monthly_summary_ledger AS
SELECT
    dr.tenant_id,
    DATE_FORMAT(dr.work_date, '%Y-%m') AS target_month,
    dr.work_date,
    dr.customer_name,
    dr.site_name,
    dr.job_code,
    dr.job_name,
    dr.site_role_code,
    dr.site_role_name,
    dr.billing_unit,
    COUNT(DISTINCT dr.employee_id) AS person_count,
    COALESCE(SUM(dr.overtime_hours), 0) AS overtime_hours,
    COALESCE(SUM(dr.night_work_hours), 0) AS night_work_hours,
    COALESCE(SUM(dr.holiday_work_hours), 0) AS holiday_work_hours,
    COALESCE(
        SUM(
            COALESCE(dr.mileage, 0)
                * COALESCE(dr.billing_commute_unit_price, 0)
            + COALESCE(dr.holiday_work_hours, 0)
                * COALESCE(dr.billing_holiday_unit_price, 0)
        ),
        0
    ) AS other_amount,
    dr.billing_base_unit_price AS base_unit_price,
    dr.billing_overtime_unit_price AS overtime_unit_price,
    dr.billing_night_unit_price AS night_unit_price,
    dr.billing_holiday_unit_price AS holiday_unit_price,
    COALESCE(SUM(dr.estimated_gross_pay_amount), 0)
        AS estimated_gross_pay_amount
FROM daily_report dr
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
GROUP BY
    dr.tenant_id,
    DATE_FORMAT(dr.work_date, '%Y-%m'),
    dr.work_date,
    dr.customer_name,
    dr.site_name,
    dr.job_code,
    dr.job_name,
    dr.site_role_code,
    dr.site_role_name,
    dr.billing_unit,
    dr.billing_base_unit_price,
    dr.billing_overtime_unit_price,
    dr.billing_night_unit_price,
    dr.billing_holiday_unit_price;

INSERT INTO excel_book_data_source_catalog (
    source_code,
    display_name,
    physical_name,
    where_clause_template,
    tenant_scoped_flag,
    max_rows,
    description,
    active_flag,
    created_at,
    updated_at,
    tenant_id
) VALUES (
    'MONTHLY_SUMMARY_LEDGER',
    '月間集計表',
    'vw_monthly_summary_ledger',
    'tenant_id = :tenantId AND target_month = :targetMonth',
    1,
    10000,
    '承認済み日報を顧客・現場・職種・現場役職・適用単価単位で月次集計する台帳専用View',
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    'default'
)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    physical_name = VALUES(physical_name),
    where_clause_template = VALUES(where_clause_template),
    max_rows = VALUES(max_rows),
    description = VALUES(description),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO excel_book_master (
    book_code, book_name,
    template_file_path, output_file_path,
    source_type, layout_type, renderer_key,
    selection_mode, selection_source_name,
    selection_value_column, selection_display_columns,
    allow_select_all, generation_unit,
    print_paper_size, print_orientation, print_fit_to_one_page,
    source_name, template_sheet_name, active_flag,
    created_at, updated_at, deleted_at, tenant_id
) VALUES (
    'MONTHLY_SUMMARY',
    '月間集計表',
    'ledgers/default/MONTHLY_SUMMARY/template.json',
    'ledgers/default/MONTHLY_SUMMARY/',
    'SNAPSHOT',
    'MONTHLY_SUMMARY',
    'MONTHLY_SUMMARY',
    'NONE',
    NULL,
    NULL,
    NULL,
    0,
    'ONE_FILE',
    'A3',
    'LANDSCAPE',
    1,
    'MONTHLY_SUMMARY_LEDGER',
    'TEMPLATE',
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL,
    'default'
)
ON DUPLICATE KEY UPDATE
    book_name = VALUES(book_name),
    template_file_path = VALUES(template_file_path),
    output_file_path = VALUES(output_file_path),
    source_type = VALUES(source_type),
    layout_type = VALUES(layout_type),
    renderer_key = VALUES(renderer_key),
    selection_mode = VALUES(selection_mode),
    selection_source_name = VALUES(selection_source_name),
    selection_value_column = VALUES(selection_value_column),
    selection_display_columns = VALUES(selection_display_columns),
    allow_select_all = VALUES(allow_select_all),
    generation_unit = VALUES(generation_unit),
    print_paper_size = VALUES(print_paper_size),
    print_orientation = VALUES(print_orientation),
    print_fit_to_one_page = VALUES(print_fit_to_one_page),
    source_name = VALUES(source_name),
    template_sheet_name = VALUES(template_sheet_name),
    active_flag = VALUES(active_flag),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP(6);

SET @monthly_summary_catalog_id = (
    SELECT id
    FROM excel_book_data_source_catalog
    WHERE tenant_id = 'default'
      AND source_code = 'MONTHLY_SUMMARY_LEDGER'
      AND deleted_at IS NULL
    LIMIT 1
);

INSERT INTO excel_book_data_source_catalog_column (
    catalog_id,
    column_name,
    display_name,
    data_type,
    order_no,
    active_flag,
    created_at,
    updated_at,
    tenant_id
) VALUES
    (@monthly_summary_catalog_id, 'work_date', '日付', 'DATE', 1, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'customer_name', '得意先名', 'STRING', 2, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'site_name', '現場名', 'STRING', 3, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'job_code', '職種コード', 'STRING', 4, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'job_name', '職種名', 'STRING', 5, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'site_role_code', '現場役職コード', 'STRING', 6, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'site_role_name', '現場役職名', 'STRING', 7, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'billing_unit', '請求単位', 'STRING', 8, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'person_count', '人数', 'NUMBER', 9, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'overtime_hours', '残業時間', 'NUMBER', 10, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'night_work_hours', '深夜時間', 'NUMBER', 11, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'holiday_work_hours', '休日時間', 'NUMBER', 12, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'other_amount', '通勤・休日加算額', 'NUMBER', 13, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'base_unit_price', '基本単価', 'NUMBER', 14, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'overtime_unit_price', '残業単価', 'NUMBER', 15, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'night_unit_price', '深夜単価', 'NUMBER', 16, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'holiday_unit_price', '休日単価', 'NUMBER', 17, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_summary_catalog_id, 'estimated_gross_pay_amount', '支払給計', 'NUMBER', 18, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    data_type = VALUES(data_type),
    order_no = VALUES(order_no),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6);

-- 台帳マスタは上記INSERTで冪等登録する。
-- 変数マッピングは不要（固定レイアウトRendererが配置する）。
