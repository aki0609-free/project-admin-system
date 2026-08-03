-- ProjectAdminSystem V1
-- 労務費支払一覧（支払周期別・従業員横持ちSpreadsheet台帳）

CREATE OR REPLACE VIEW vw_labor_cost_payment_deduction AS
SELECT
    drd.tenant_id,
    drd.daily_report_id,
    COALESCE(SUM(CASE
        WHEN drd.deduction_code = 'INCOME_TAX' THEN drd.amount
        ELSE 0
    END), 0) AS income_tax_amount,
    COALESCE(SUM(CASE
        WHEN drd.deduction_code NOT IN (
            'INCOME_TAX', 'SAVINGS', 'LOAN_REPAYMENT'
        ) THEN drd.amount
        ELSE 0
    END), 0) AS other_deduction_amount
FROM daily_report_deductions drd
WHERE drd.deleted_at IS NULL
GROUP BY drd.tenant_id, drd.daily_report_id;

-- 当月の承認済み新規貸付を、原本の「借金・引出」にマイナス表示する。
-- 貯金引出はV1では履歴テーブルがないため0円とする。
CREATE OR REPLACE VIEW vw_labor_cost_payment_borrowing AS
SELECT
    loan.tenant_id,
    DATE_FORMAT(loan.loan_date, '%Y-%m') AS target_month,
    loan.employee_id,
    COALESCE(SUM(loan.principal) * -1, 0)
        AS borrow_withdrawal_amount
FROM employee_loan loan
WHERE loan.deleted_at IS NULL
  AND loan.approval_status = 'APPROVED'
  AND loan.loan_date IS NOT NULL
GROUP BY
    loan.tenant_id,
    DATE_FORMAT(loan.loan_date, '%Y-%m'),
    loan.employee_id;

CREATE OR REPLACE VIEW vw_labor_cost_payment_ledger AS
SELECT
    dr.tenant_id,
    DATE_FORMAT(dr.work_date, '%Y-%m') AS target_month,
    dr.employee_id,
    e.employee_code,
    e.employee_name,
    COALESCE(contract.payment_cycle, 'MONTHLY') AS payment_cycle,
    company.company_name,
    dr.work_date,
    COALESCE(SUM(dr.estimated_gross_pay_amount), 0)
        AS daily_gross_amount,
    COALESCE(SUM(deduction.income_tax_amount), 0)
        AS income_tax_amount,
    COALESCE(SUM(
        COALESCE(dr.saving_amount, 0)
            + COALESCE(dr.loan_repayment_amount, 0)
    ), 0) AS saving_repayment_amount,
    COALESCE(borrowing.borrow_withdrawal_amount, 0)
        AS borrow_withdrawal_amount,
    COALESCE(SUM(deduction.other_deduction_amount), 0)
        AS other_deduction_amount
FROM daily_report dr
JOIN employee e
  ON e.id = dr.employee_id
 AND e.tenant_id = dr.tenant_id
 AND e.deleted_at IS NULL
LEFT JOIN employee_contract contract
  ON contract.employee_id = dr.employee_id
 AND contract.tenant_id = dr.tenant_id
 AND contract.deleted_at IS NULL
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
  ON company.tenant_id = dr.tenant_id
LEFT JOIN vw_labor_cost_payment_deduction deduction
  ON deduction.tenant_id = dr.tenant_id
 AND deduction.daily_report_id = dr.id
LEFT JOIN vw_labor_cost_payment_borrowing borrowing
  ON borrowing.tenant_id = dr.tenant_id
 AND borrowing.employee_id = dr.employee_id
 AND borrowing.target_month = DATE_FORMAT(dr.work_date, '%Y-%m')
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
GROUP BY
    dr.tenant_id,
    DATE_FORMAT(dr.work_date, '%Y-%m'),
    dr.employee_id,
    e.employee_code,
    e.employee_name,
    COALESCE(contract.payment_cycle, 'MONTHLY'),
    company.company_name,
    dr.work_date,
    borrowing.borrow_withdrawal_amount;

INSERT INTO excel_book_data_source_catalog (
    source_code, display_name, physical_name,
    where_clause_template, tenant_scoped_flag, max_rows,
    description, active_flag, created_at, updated_at, tenant_id
) VALUES (
    'LABOR_COST_PAYMENT_LEDGER',
    '労務費支払一覧',
    'vw_labor_cost_payment_ledger',
    'tenant_id = :tenantId AND target_month = :targetMonth',
    1,
    10000,
    '対象月の総支給・控除を支払周期別、従業員別、日別に返す専用View',
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
    'LABOR_COST_PAYMENT',
    '労務費支払一覧',
    'ledgers/default/LABOR_COST_PAYMENT/template.json',
    'ledgers/default/LABOR_COST_PAYMENT/',
    'SNAPSHOT',
    'DEDICATED',
    'LABOR_COST_PAYMENT_V1',
    'NONE',
    NULL,
    NULL,
    NULL,
    0,
    'ONE_FILE',
    'A4',
    'LANDSCAPE',
    1,
    'LABOR_COST_PAYMENT_LEDGER',
    'LABOR_COST_PAYMENT',
    1,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6),
    NULL,
    'default'
)
ON DUPLICATE KEY UPDATE
    book_name = VALUES(book_name),
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

SET @labor_cost_payment_catalog_id = (
    SELECT id
    FROM excel_book_data_source_catalog
    WHERE tenant_id = 'default'
      AND source_code = 'LABOR_COST_PAYMENT_LEDGER'
      AND deleted_at IS NULL
    LIMIT 1
);

INSERT INTO excel_book_data_source_catalog_column (
    catalog_id, column_name, display_name, data_type,
    order_no, active_flag, created_at, updated_at, tenant_id
) VALUES
    (@labor_cost_payment_catalog_id, 'employee_id', '従業員ID', 'NUMBER', 1, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@labor_cost_payment_catalog_id, 'employee_code', '従業員番号', 'STRING', 2, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@labor_cost_payment_catalog_id, 'employee_name', '従業員名', 'STRING', 3, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@labor_cost_payment_catalog_id, 'payment_cycle', '支払い方法', 'STRING', 4, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@labor_cost_payment_catalog_id, 'company_name', '会社名', 'STRING', 5, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@labor_cost_payment_catalog_id, 'work_date', '日付', 'DATE', 6, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@labor_cost_payment_catalog_id, 'daily_gross_amount', '日別総支給', 'NUMBER', 7, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@labor_cost_payment_catalog_id, 'income_tax_amount', '所得税', 'NUMBER', 8, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@labor_cost_payment_catalog_id, 'borrow_withdrawal_amount', '借金・引出', 'NUMBER', 9, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@labor_cost_payment_catalog_id, 'saving_repayment_amount', '貯金・返済', 'NUMBER', 10, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@labor_cost_payment_catalog_id, 'other_deduction_amount', 'その他控除', 'NUMBER', 11, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    data_type = VALUES(data_type),
    order_no = VALUES(order_no),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6);

-- V1制約:
-- 貯金の引出履歴は未保持のため、「借金・引出」には当月の承認済み
-- 新規貸付だけをマイナス値で表示する。
