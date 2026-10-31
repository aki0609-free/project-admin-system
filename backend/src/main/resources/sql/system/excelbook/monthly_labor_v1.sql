-- ProjectAdminSystem V1
-- 月間労務表（対象月＋従業員ごとに1台帳）
SET NAMES utf8mb4;

CREATE OR REPLACE VIEW vw_monthly_labor_ledger_deduction AS
SELECT
    drd.tenant_id,
    drd.daily_report_id,
    COALESCE(SUM(CASE
        WHEN drd.deduction_code = 'LEGAL_DEPOSIT' THEN drd.amount
        ELSE 0
    END), 0) AS legal_deposit_amount,
    COALESCE(SUM(CASE
        WHEN drd.deduction_code = 'SHORT_TERM_ADVANCE' THEN drd.amount
        ELSE 0
    END), 0) AS short_term_advance_amount,
    COALESCE(SUM(CASE
        WHEN drd.deduction_code = 'DORMITORY_FEE' THEN drd.amount
        ELSE 0
    END), 0) AS dormitory_fee_amount,
    COALESCE(SUM(CASE
        WHEN drd.deduction_code = 'MOBILE_RENTAL' THEN drd.amount
        ELSE 0
    END), 0) AS mobile_rental_amount,
    COALESCE(SUM(CASE
        WHEN drd.deduction_code = 'WIFI_FEE' THEN drd.amount
        ELSE 0
    END), 0) AS wifi_fee_amount,
    COALESCE(SUM(CASE
        WHEN COALESCE(dm.deduction_type, '') <> 'LEGAL'
         AND drd.deduction_code NOT IN (
            'INCOME_TAX',
            'LEGAL_DEPOSIT',
            'ADVANCE_PAYMENT',
            'SHORT_TERM_ADVANCE',
            'DORMITORY_FEE',
            'MOBILE_RENTAL',
            'WIFI_FEE',
            'SAVINGS',
            'LOAN_REPAYMENT'
         ) THEN drd.amount
        ELSE 0
    END), 0) AS other_deduction_amount
FROM daily_report_deductions drd
LEFT JOIN deduction_masters dm
  ON dm.id = drd.deduction_master_id
 AND dm.tenant_id = drd.tenant_id
 AND dm.deleted_at IS NULL
WHERE drd.deleted_at IS NULL
GROUP BY drd.tenant_id, drd.daily_report_id;

CREATE OR REPLACE VIEW vw_monthly_labor_ledger_allowance AS
SELECT
    dra.tenant_id,
    dra.daily_report_id,
    COALESCE(SUM(CASE
        WHEN dra.allowance_code = 'DRIVER_ALLOWANCE' THEN dra.amount
        ELSE 0
    END), 0) AS driver_allowance_amount,
    COALESCE(SUM(CASE
        WHEN dra.allowance_code = 'ATTENDANCE_ATTITUDE' THEN dra.amount
        ELSE 0
    END), 0) AS attendance_attitude_allowance_amount,
    COALESCE(SUM(CASE
        WHEN dra.allowance_code = 'MANAGEMENT_ALLOWANCE' THEN dra.amount
        ELSE 0
    END), 0) AS management_allowance_amount,
    COALESCE(SUM(CASE
        WHEN dra.allowance_code NOT IN (
            'DRIVER_ALLOWANCE',
            'ATTENDANCE_ATTITUDE',
            'MANAGEMENT_ALLOWANCE'
        ) THEN dra.amount
        ELSE 0
    END), 0) AS other_allowance_amount
FROM daily_report_allowances dra
WHERE dra.deleted_at IS NULL
GROUP BY dra.tenant_id, dra.daily_report_id;

CREATE OR REPLACE VIEW vw_monthly_labor_ledger AS
SELECT
    dr.tenant_id,
    DATE_FORMAT(dr.work_date, '%Y-%m') AS target_month,
    dr.employee_id,
    e.employee_code,
    e.employee_name,
    company.company_name,
    dr.work_date,
    GROUP_CONCAT(
        DISTINCT NULLIF(dr.customer_name, '')
        ORDER BY dr.customer_name
        SEPARATOR ' / '
    ) AS customer_name,
    GROUP_CONCAT(
        DISTINCT NULLIF(dr.site_name, '')
        ORDER BY dr.site_name
        SEPARATOR ' / '
    ) AS site_name,
    CONCAT(
        COALESCE(DATE_FORMAT(MIN(dr.start_time), '%H:%i'), ''),
        CASE
            WHEN MIN(dr.start_time) IS NULL AND MAX(dr.end_time) IS NULL THEN ''
            ELSE '～'
        END,
        COALESCE(DATE_FORMAT(MAX(dr.end_time), '%H:%i'), '')
    ) AS work_time,
    COALESCE(SUM(dr.work_hours), 0) AS work_hours,
    COALESCE(SUM(dr.overtime_hours), 0) AS overtime_hours,
    COALESCE(SUM(dr.night_work_hours), 0) AS night_work_hours,
    COALESCE(SUM(dr.work_hours), 0)
        + COALESCE(SUM(dr.overtime_hours), 0)
        + COALESCE(SUM(dr.holiday_work_hours), 0) AS total_hours,
    COALESCE(SUM(dr.normal_pay_amount), 0) AS normal_pay_amount,
    COALESCE(SUM(dr.overtime_pay_amount), 0) AS overtime_pay_amount,
    COALESCE(SUM(dr.night_pay_amount), 0) AS night_pay_amount,
    COALESCE(SUM(allowance.driver_allowance_amount), 0)
        AS driver_allowance_amount,
    COALESCE(SUM(allowance.attendance_attitude_allowance_amount), 0)
        AS attendance_attitude_allowance_amount,
    COALESCE(SUM(allowance.management_allowance_amount), 0)
        AS management_allowance_amount,
    COALESCE(SUM(allowance.other_allowance_amount), 0)
        AS other_allowance_amount,
    COALESCE(SUM(dr.estimated_gross_pay_amount), 0) AS gross_pay_amount,
    COALESCE(SUM(deduction.legal_deposit_amount), 0)
        AS legal_deposit_amount,
    COALESCE(SUM(dr.saving_amount), 0) AS saving_amount,
    COALESCE(SUM(dr.saving_withdrawal_amount), 0)
        AS saving_withdrawal_amount,
    COALESCE(SUM(deduction.short_term_advance_amount), 0)
        AS short_term_advance_amount,
    COALESCE(SUM(dr.loan_repayment_amount), 0) AS loan_repayment_amount,
    COALESCE(SUM(deduction.dormitory_fee_amount), 0)
        AS dormitory_fee_amount,
    COALESCE(SUM(deduction.mobile_rental_amount), 0)
        AS mobile_rental_amount,
    COALESCE(SUM(deduction.wifi_fee_amount), 0) AS wifi_fee_amount,
    COALESCE(SUM(deduction.other_deduction_amount), 0)
        AS other_deduction_amount,
    GROUP_CONCAT(
        DISTINCT NULLIF(dr.work_description, '')
        ORDER BY dr.id
        SEPARATOR ' / '
    ) AS note
FROM daily_report dr
JOIN employee e
  ON e.id = dr.employee_id
 AND e.tenant_id = dr.tenant_id
 AND e.deleted_at IS NULL
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
LEFT JOIN vw_monthly_labor_ledger_allowance allowance
  ON allowance.tenant_id = dr.tenant_id
 AND allowance.daily_report_id = dr.id
LEFT JOIN vw_monthly_labor_ledger_deduction deduction
  ON deduction.tenant_id = dr.tenant_id
 AND deduction.daily_report_id = dr.id
WHERE dr.deleted_at IS NULL
  AND dr.approval_status = 'APPROVED'
GROUP BY
    dr.tenant_id,
    DATE_FORMAT(dr.work_date, '%Y-%m'),
    dr.employee_id,
    e.employee_code,
    e.employee_name,
    company.company_name,
    dr.work_date;

DROP VIEW IF EXISTS vw_monthly_labor_ledger_payment;

INSERT INTO excel_book_data_source_catalog (
    source_code, display_name, physical_name,
    where_clause_template, tenant_scoped_flag, max_rows,
    description, active_flag, created_at, updated_at, tenant_id
) VALUES (
    'MONTHLY_LABOR_LEDGER',
    '月間労務表',
    'vw_monthly_labor_ledger',
    'tenant_id = :tenantId AND target_month = :targetMonth',
    1,
    5000,
    '対象月・従業員別の日報給与確定値を返す月間労務表専用View',
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
    'MONTHLY_LABOR',
    '月間労務表',
    'ledgers/default/MONTHLY_LABOR/template.json',
    'ledgers/default/MONTHLY_LABOR/',
    'SNAPSHOT',
    'DEDICATED',
    'MONTHLY_LABOR_V1',
    'MULTIPLE',
    'MONTHLY_LABOR_LEDGER',
    'employee_id',
    'employee_code,employee_name',
    1,
    'FILE_PER_SELECTION',
    'A3',
    'LANDSCAPE',
    1,
    'MONTHLY_LABOR_LEDGER',
    'MONTHLY_LABOR',
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

SET @monthly_labor_catalog_id = (
    SELECT id
    FROM excel_book_data_source_catalog
    WHERE tenant_id = 'default'
      AND source_code = 'MONTHLY_LABOR_LEDGER'
      AND deleted_at IS NULL
    LIMIT 1
);

INSERT INTO excel_book_data_source_catalog_column (
    catalog_id, column_name, display_name, data_type,
    order_no, active_flag, created_at, updated_at, tenant_id
) VALUES
    (@monthly_labor_catalog_id, 'employee_id', '従業員ID', 'NUMBER', 1, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'employee_code', '従業員コード', 'STRING', 2, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'employee_name', '従業員名', 'STRING', 3, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'work_date', '日付', 'DATE', 4, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'customer_name', '元請', 'STRING', 5, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'site_name', '現場', 'STRING', 6, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'work_time', '勤務時間', 'STRING', 7, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'total_hours', '時間', 'NUMBER', 8, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'normal_pay_amount', '通常給金', 'NUMBER', 9, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'overtime_pay_amount', '早出・残業', 'NUMBER', 10, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'night_pay_amount', '深夜', 'NUMBER', 11, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'driver_allowance_amount', '運転手当', 'NUMBER', 12, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'attendance_attitude_allowance_amount', '勤務態度手当', 'NUMBER', 13, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'management_allowance_amount', '管理手当', 'NUMBER', 14, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'other_allowance_amount', 'その他手当', 'NUMBER', 15, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'gross_pay_amount', '支払給A', 'NUMBER', 16, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'legal_deposit_amount', '法定準備金', 'NUMBER', 17, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'saving_amount', '貯金', 'NUMBER', 18, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'saving_withdrawal_amount', '引出', 'NUMBER', 19, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'short_term_advance_amount', '前借り', 'NUMBER', 20, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'loan_repayment_amount', '借入金返済額', 'NUMBER', 21, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'dormitory_fee_amount', '寮費', 'NUMBER', 22, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'mobile_rental_amount', '携帯電話貸出料', 'NUMBER', 23, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'wifi_fee_amount', 'Wi-Fi使用料', 'NUMBER', 24, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'other_deduction_amount', 'その他', 'NUMBER', 25, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'note', '備考', 'STRING', 26, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default'),
    (@monthly_labor_catalog_id, 'company_name', '会社名', 'STRING', 27, 1, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 'default')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    data_type = VALUES(data_type),
    order_no = VALUES(order_no),
    active_flag = VALUES(active_flag),
    updated_at = CURRENT_TIMESTAMP(6);

UPDATE excel_book_data_source_catalog_column
SET active_flag = 0,
    updated_at = CURRENT_TIMESTAMP(6)
WHERE catalog_id = @monthly_labor_catalog_id
  AND column_name IN (
      'holiday_pay_amount',
      'vehicle_allowance_amount',
      'tax_other_amount',
      'advance_payment_amount',
      'available_payment_amount',
      'other_daily_amount'
  );

-- 台帳マスタ設定値:
-- bookCode                 : MONTHLY_LABOR
-- rendererKey              : MONTHLY_LABOR_V1
-- layoutType               : DEDICATED
-- dataSourceCode           : MONTHLY_LABOR_LEDGER
-- selectionMode            : MULTIPLE
-- selectionSourceName      : MONTHLY_LABOR_LEDGER
-- selectionValueColumn     : employee_id
-- selectionDisplayColumns  : employee_code,employee_name
-- allowSelectAll           : true
-- generationUnit           : FILE_PER_SELECTION
-- printPaperSize           : A3
-- printOrientation         : LANDSCAPE
-- printFitToOnePage        : true
