SET NAMES utf8mb4;

create table if not exists daily_pay_slip_input (
    id bigint auto_increment primary key,
    tenant_id varchar(100) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6) null,

    execution_id varchar(100) not null,
    payment_date date not null,
    -- NULL means all employees for the selected payment date.
    employee_id bigint null,

    index idx_daily_pay_slip_input_execution (execution_id),
    index idx_daily_pay_slip_input_target (tenant_id, payment_date)
);

-- Existing environments may still have the former NOT NULL definition.
-- Keep this migration idempotent so the runtime schema upgrade also repairs them.
alter table daily_pay_slip_input
    modify column employee_id bigint null;

create table if not exists daily_pay_slip_output (
    id bigint auto_increment primary key,
    tenant_id varchar(100) not null,
    created_at datetime(6) null,
    updated_at datetime(6) null,
    deleted_at datetime(6) null,

    execution_id varchar(100) not null,
    payment_date date not null,

    employee_id bigint null,
    employee_code varchar(100) null,
    employee_name varchar(200) null,
    recipient_key varchar(255) null,
    recipient_name varchar(255) null,
    recipient_email varchar(255) null,
    business_key varchar(255) null,
    mail_type varchar(100) null,
    mail_template_key varchar(100) null,

    work_date date null,
    labor_period_from date null,
    labor_period_to date null,

    work_hours decimal(10,2) null,
    overtime_hours decimal(10,2) null,
    night_work_hours decimal(10,2) null,

    basic_salary decimal(15,2) null,

    allowance_item_name1 varchar(100) null,
    allowance_item_value1 decimal(15,2) null,
    allowance_item_name2 varchar(100) null,
    allowance_item_value2 decimal(15,2) null,
    allowance_item_name3 varchar(100) null,
    allowance_item_value3 decimal(15,2) null,
    allowance_item_name4 varchar(100) null,
    allowance_item_value4 decimal(15,2) null,
    allowance_item_name5 varchar(100) null,
    allowance_item_value5 decimal(15,2) null,
    allowance_item_name6 varchar(100) null,
    allowance_item_value6 decimal(15,2) null,
    allowance_item_name7 varchar(100) null,
    allowance_item_value7 decimal(15,2) null,
    allowance_item_name8 varchar(100) null,
    allowance_item_value8 decimal(15,2) null,
    allowance_item_name9 varchar(100) null,
    allowance_item_value9 decimal(15,2) null,
    allowance_item_name10 varchar(100) null,
    allowance_item_value10 decimal(15,2) null,

    deduction_item_name1 varchar(100) null,
    deduction_item_value1 decimal(15,2) null,
    deduction_item_name2 varchar(100) null,
    deduction_item_value2 decimal(15,2) null,
    deduction_item_name3 varchar(100) null,
    deduction_item_value3 decimal(15,2) null,
    deduction_item_name4 varchar(100) null,
    deduction_item_value4 decimal(15,2) null,
    deduction_item_name5 varchar(100) null,
    deduction_item_value5 decimal(15,2) null,
    deduction_item_name6 varchar(100) null,
    deduction_item_value6 decimal(15,2) null,
    deduction_item_name7 varchar(100) null,
    deduction_item_value7 decimal(15,2) null,
    deduction_item_name8 varchar(100) null,
    deduction_item_value8 decimal(15,2) null,
    deduction_item_name9 varchar(100) null,
    deduction_item_value9 decimal(15,2) null,
    deduction_item_name10 varchar(100) null,
    deduction_item_value10 decimal(15,2) null,

    gross_amount decimal(15,2) null,
    allowance_total decimal(15,2) null,
    deduction_total decimal(15,2) null,
    daily_payment_amount decimal(15,2) null,
    net_payment_amount decimal(15,2) null,
    note varchar(1000) null,

    index idx_daily_pay_slip_output_execution (execution_id),
    index idx_daily_pay_slip_output_delivery (execution_id, business_key),
    index idx_daily_pay_slip_output_employee (
        tenant_id,
        payment_date,
        employee_id
    )
);

create table if not exists monthly_pay_slip_input (
    id bigint auto_increment primary key,
    tenant_id varchar(100) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6) null,

    execution_id varchar(100) not null,
    target_month varchar(7) not null,
    employee_id bigint null,

    index idx_monthly_pay_slip_input_execution (execution_id),
    index idx_monthly_pay_slip_input_target (tenant_id, target_month)
);

create table if not exists monthly_pay_slip_output (
    id bigint auto_increment primary key,
    tenant_id varchar(100) not null,
    created_at datetime(6) null,
    updated_at datetime(6) null,
    deleted_at datetime(6) null,

    execution_id varchar(100) not null,
    target_month varchar(7) not null,

    employee_id bigint null,
    employee_code varchar(100) null,
    employee_name varchar(200) null,
    recipient_key varchar(255) null,
    recipient_name varchar(255) null,
    recipient_email varchar(255) null,
    business_key varchar(255) null,
    mail_type varchar(100) null,
    mail_template_key varchar(100) null,

    work_day_count int null,
    overtime_hours decimal(10,2) null,
    night_work_hours decimal(10,2) null,

    basic_salary decimal(15,2) null,
    overtime_allowance decimal(15,2) null,
    night_allowance decimal(15,2) null,
    attendance_allowance decimal(15,2) null,
    driver_allowance decimal(15,2) null,
    manager_allowance decimal(15,2) null,
    other_allowance decimal(15,2) null,

    health_insurance decimal(15,2) null,
    child_care_contribution decimal(15,2) null,
    pension_insurance decimal(15,2) null,
    employment_insurance decimal(15,2) null,
    social_insurance_total decimal(15,2) null,
    taxable_amount decimal(15,2) null,
    income_tax decimal(15,2) null,
    resident_tax decimal(15,2) null,
    other_deduction decimal(15,2) null,

    gross_amount decimal(15,2) null,
    deduction_total decimal(15,2) null,
    net_payment_amount decimal(15,2) null,

    index idx_monthly_pay_slip_output_execution (execution_id),
    index idx_monthly_pay_slip_output_delivery (execution_id, business_key)
);

set @tenant_id = 'default';
set @now = now();

-- =====================================================
-- 既存削除：MONTHLY_PAY_SLIP / DAILY_PAY_SLIP
-- =====================================================

delete rp
from report_param rp
join report_master rm
  on rm.id = rp.report_master_id
where rm.tenant_id = @tenant_id
  and rm.report_code in (
    'MONTHLY_PAY_SLIP',
    'DAILY_PAY_SLIP'
  );

delete from report_master
where tenant_id = @tenant_id
  and report_code in (
    'MONTHLY_PAY_SLIP',
    'DAILY_PAY_SLIP'
  );

-- =====================================================
-- 月給料明細
-- =====================================================

insert into report_master (
    tenant_id,
    created_at,
    updated_at,
    report_code,
    report_name,
    template_file_name,
    work_table,
    input_table,
    output_table,
    source_view_name,
    history_table,
    pre_process_type,
    pre_process_sql,
    procedure_name,
    query_sql,
    cleanup_type,
    cleanup_sql,
    cleanup_procedure_name,
    layout_type,
    layout_count,
    file_name,
    output_format,
    use_signature,
    preview_enabled,
    active_flag
) values (
    @tenant_id,
    @now,
    @now,
    'MONTHLY_PAY_SLIP',
    '月給料明細',
    'monthly_pay_slip.jrxml',
    'monthly_pay_slip',
    'monthly_pay_slip_input',
    'monthly_pay_slip_render_output',
    'vw_monthly_pay_slip_latest',
    'monthly_pay_slip_history',
    'PROCEDURE',
    null,
    'sp_monthly_pay_slip_snapshot',
    'select *
from vw_monthly_pay_slip_render_flat
where execution_id = :executionId
order by employee_code',
    'PROCEDURE',
    null,
    'sp_monthly_pay_slip_cleanup',
    'SINGLE',
    1,
    '月給料明細_${targetMonth}',
    'PDF',
    false,
    true,
    true
);

set @monthly_report_master_id = (
    select id
    from report_master
    where tenant_id = @tenant_id
      and report_code = 'MONTHLY_PAY_SLIP'
    limit 1
);

insert into report_param (
    tenant_id,
    created_at,
    updated_at,
    report_master_id,
    param_name,
    param_label,
    param_type,
    control_type,
    required_flag,
    visible_flag,
    multiple_flag,
    filter_flag,
    default_value,
    placeholder,
    input_column_name,
    display_order,
    active_flag
) values
(
    @tenant_id,
    @now,
    @now,
    @monthly_report_master_id,
    'targetMonth',
    '対象月',
    'STRING',
    'TEXT',
    true,
    true,
    false,
    true,
    null,
    '対象月を選択',
    'target_month',
    1,
    true
),
(
    @tenant_id,
    @now,
    @now,
    @monthly_report_master_id,
    'employeeId',
    '従業員',
    'LONG',
    'TEXT',
    false,
    true,
    false,
    true,
    null,
    '未指定の場合は全員',
    'employee_id',
    2,
    true
),
(
    @tenant_id,
    @now,
    @now,
    @monthly_report_master_id,
    'closingVersion',
    '締めVersion',
    'LONG',
    'NUMBER',
    true,
    false,
    false,
    false,
    null,
    null,
    'closing_version',
    3,
    true
),
(
    @tenant_id,
    @now,
    @now,
    @monthly_report_master_id,
    'executionMode',
    '実行モード',
    'STRING',
    'TEXT',
    true,
    false,
    false,
    false,
    null,
    null,
    'execution_mode',
    4,
    true
);

-- =====================================================
-- 日払い明細
-- =====================================================

insert into report_master (
    tenant_id,
    created_at,
    updated_at,
    report_code,
    report_name,
    template_file_name,
    work_table,
    input_table,
    output_table,
    source_view_name,
    history_table,
    pre_process_type,
    pre_process_sql,
    procedure_name,
    query_sql,
    cleanup_type,
    cleanup_sql,
    cleanup_procedure_name,
    layout_type,
    layout_count,
    file_name,
    output_format,
    use_signature,
    preview_enabled,
    active_flag
) values (
    @tenant_id,
    @now,
    @now,
    'DAILY_PAY_SLIP',
    '日払い明細',
    'daily_pay_slip.jrxml',
    'daily_pay_slip',
    'daily_pay_slip_input',
    'daily_pay_slip_output',
    'vw_daily_pay_slip_latest',
    null,
    'PROCEDURE',
    null,
    'sp_daily_pay_slip_prepare',
    'select *
from daily_pay_slip_output
where execution_id = :executionId
order by employee_code',
    'PROCEDURE',
    null,
    'sp_daily_pay_slip_cleanup',
    'SINGLE',
    1,
    '日払い明細_${paymentDate}',
    'PDF',
    false,
    true,
    true
);

set @daily_report_master_id = (
    select id
    from report_master
    where tenant_id = @tenant_id
      and report_code = 'DAILY_PAY_SLIP'
    limit 1
);

insert into report_param (
    tenant_id,
    created_at,
    updated_at,
    report_master_id,
    param_name,
    param_label,
    param_type,
    control_type,
    required_flag,
    visible_flag,
    multiple_flag,
    filter_flag,
    default_value,
    placeholder,
    input_column_name,
    display_order,
    active_flag
) values
(
    @tenant_id,
    @now,
    @now,
    @daily_report_master_id,
    'paymentDate',
    '支払日',
    'DATE',
    'DATE',
    true,
    true,
    false,
    true,
    null,
    '支払日を選択',
    'payment_date',
    1,
    true
),
(
    @tenant_id,
    @now,
    @now,
    @daily_report_master_id,
    'employeeId',
    '従業員',
    'LONG',
    'TEXT',
    false,
    true,
    false,
    true,
    null,
    '未指定の場合は全員',
    'employee_id',
    2,
    true
);
