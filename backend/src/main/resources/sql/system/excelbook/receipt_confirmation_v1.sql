-- ProjectAdminSystem V1
-- 入金確認表（請求月確定後も入金完了まで更新するSpreadsheet台帳）

CREATE OR REPLACE VIEW vw_receipt_confirmation_ledger AS
SELECT
    ct.tenant_id,
    ct.target_month,
    ct.id AS transaction_id,
    ct.customer_id,
    customer.name AS customer_name,
    company.company_name,
    CONCAT(
        '毎月 ',
        CASE ct.closing_day_type
            WHEN 'END_OF_MONTH' THEN '末'
            ELSE CAST(ct.closing_day_value AS CHAR)
        END,
        '日'
    ) AS closing_rule_text,
    CONCAT(
        CASE ct.payment_month_offset
            WHEN 0 THEN '当月 '
            WHEN 1 THEN '翌 '
            WHEN 2 THEN '翌々 '
            ELSE CONCAT(
                ct.payment_month_offset,
                'か月後 '
            )
        END,
        CASE ct.payment_day_type
            WHEN 'END_OF_MONTH' THEN '末'
            ELSE CAST(ct.payment_day_value AS CHAR)
        END,
        '日'
    ) AS payment_rule_text,
    COALESCE(ct.billing_amount, 0) AS billing_amount,
    ct.expected_payment_date,
    ct.confirmed_payment_date,
    COALESCE(ct.paid_amount, 0) AS paid_amount,
    COALESCE(ct.fee, 0) AS fee,
    COALESCE(ct.offset_amount, 0) AS offset_amount,
    COALESCE(ct.adjustment_amount, 0) AS adjustment_amount,
    COALESCE(ct.paid_amount, 0)
        + COALESCE(ct.fee, 0)
        + COALESCE(ct.offset_amount, 0)
        + COALESCE(ct.adjustment_amount, 0)
        AS settled_amount,
    COALESCE(ct.billing_amount, 0)
        - (
            COALESCE(ct.paid_amount, 0)
            + COALESCE(ct.fee, 0)
            + COALESCE(ct.offset_amount, 0)
            + COALESCE(ct.adjustment_amount, 0)
        ) AS remaining_amount,
    COALESCE(ct.payment_status, 'UNPAID') AS payment_status,
    ct.note
FROM customer_transactions ct
JOIN customers customer
  ON customer.id = ct.customer_id
 AND customer.tenant_id = ct.tenant_id
 AND customer.deleted_at IS NULL
LEFT JOIN (
    SELECT
        profile.tenant_id,
        profile.company_name
    FROM company_profile profile
    JOIN (
        SELECT
            tenant_id,
            MIN(id) AS company_profile_id
        FROM company_profile
        WHERE active_flag = TRUE
          AND deleted_at IS NULL
        GROUP BY tenant_id
    ) selected
      ON selected.company_profile_id = profile.id
     AND selected.tenant_id = profile.tenant_id
) company
  ON company.tenant_id = ct.tenant_id
WHERE ct.deleted_at IS NULL;

INSERT INTO excel_book_data_source_catalog (
    source_code, display_name, physical_name,
    where_clause_template, tenant_scoped_flag, max_rows,
    description, active_flag, created_at, updated_at, tenant_id
) VALUES (
    'RECEIPT_CONFIRMATION_LEDGER',
    '入金確認表',
    'vw_receipt_confirmation_ledger',
    'tenant_id = :tenantId AND target_month = :targetMonth',
    1,
    5000,
    '請求月確定後も入金完了まで更新する顧客別入金確認View',
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
    'RECEIPT_CONFIRMATION',
    '入金確認表',
    'ledgers/default/RECEIPT_CONFIRMATION/template.json',
    'ledgers/default/RECEIPT_CONFIRMATION/',
    'SNAPSHOT',
    'DEDICATED',
    'RECEIPT_CONFIRMATION_V1',
    'NONE',
    NULL,
    NULL,
    NULL,
    0,
    'ONE_FILE',
    'A4',
    'LANDSCAPE',
    1,
    'RECEIPT_CONFIRMATION_LEDGER',
    'RECEIPT_CONFIRMATION',
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

SET @receipt_confirmation_catalog_id = (
    SELECT id
    FROM excel_book_data_source_catalog
    WHERE tenant_id = 'default'
      AND source_code = 'RECEIPT_CONFIRMATION_LEDGER'
      AND deleted_at IS NULL
    LIMIT 1
);

INSERT INTO excel_book_data_source_catalog_column (
    catalog_id, column_name, display_name, data_type,
    order_no, active_flag, created_at, updated_at, tenant_id
) VALUES
    (@receipt_confirmation_catalog_id, 'transaction_id', '取引ID', 'NUMBER', 1, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'customer_id', '顧客ID', 'NUMBER', 2, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'customer_name', '業者名', 'STRING', 3, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'company_name', '会社名', 'STRING', 4, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'closing_rule_text', '締め日', 'STRING', 5, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'payment_rule_text', '支払日', 'STRING', 6, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'billing_amount', '請求金額', 'NUMBER', 7, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'expected_payment_date', '入金予定日', 'DATE', 8, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'confirmed_payment_date', '入金確認日', 'DATE', 9, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'paid_amount', '入金額', 'NUMBER', 10, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'fee', '手数料', 'NUMBER', 11, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'offset_amount', '相殺', 'NUMBER', 12, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'adjustment_amount', 'その他調整額', 'NUMBER', 13, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'settled_amount', '合計金額', 'NUMBER', 14, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'remaining_amount', '残額', 'NUMBER', 15, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'payment_status', '入金状態', 'STRING', 16, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@receipt_confirmation_catalog_id, 'note', '備考', 'STRING', 17, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    data_type = VALUES(data_type),
    order_no = VALUES(order_no),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6);
