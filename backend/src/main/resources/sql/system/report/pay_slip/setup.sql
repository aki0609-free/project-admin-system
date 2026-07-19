create table if not exists daily_pay_slip_input (
    id bigint auto_increment primary key,
    tenant_id varchar(100) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6) null,

    execution_id varchar(100) not null,
    payment_date date not null,
    employee_id bigint null,

    index idx_daily_pay_slip_input_execution (execution_id),
    index idx_daily_pay_slip_input_target (tenant_id, payment_date)
);

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

    labor_period_from date null,
    labor_period_to date null,

    basic_salary decimal(15,2) null,
    overtime_allowance decimal(15,2) null,
    night_allowance decimal(15,2) null,
    attendance_allowance decimal(15,2) null,
    driver_allowance decimal(15,2) null,
    manager_allowance decimal(15,2) null,
    other_allowance decimal(15,2) null,

    legal_withholding_amount decimal(15,2) null,
    legal_withholding_balance decimal(15,2) null,

    gross_amount decimal(15,2) null,
    deduction_total decimal(15,2) null,
    net_payment_amount decimal(15,2) null,

    index idx_daily_pay_slip_output_execution (execution_id),
    index idx_daily_pay_slip_output_delivery (execution_id, business_key)
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
    pre_process_type,
    pre_process_sql,
    procedure_name,
    output_path,
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
    'monthly_pay_slip_output',
    'SQL',
    '
delete from monthly_pay_slip_output
where execution_id = :executionId;

insert into monthly_pay_slip_output (
    tenant_id,
    created_at,
    updated_at,
    execution_id,
    target_month,
    employee_id,
    employee_code,
    employee_name,
    recipient_key,
    recipient_name,
    recipient_email,
    business_key,
    mail_type,
    mail_template_key,
    work_day_count,
    overtime_hours,
    night_work_hours,
    basic_salary,
    overtime_allowance,
    night_allowance,
    attendance_allowance,
    driver_allowance,
    manager_allowance,
    other_allowance,
    health_insurance,
    child_care_contribution,
    pension_insurance,
    employment_insurance,
    social_insurance_total,
    taxable_amount,
    income_tax,
    resident_tax,
    other_deduction,
    gross_amount,
    deduction_total,
    net_payment_amount
)
select
    i.tenant_id,
    now(),
    now(),
    i.execution_id,
    i.target_month,
    e.id,
    e.employee_code,
    e.employee_name,
    cast(e.id as char),
    e.employee_name,
    e.email,
    concat(
        ''MONTHLY_PAY_SLIP:'',
        i.target_month,
        '':'',
        e.id
    ),
    ''MONTHLY_PAY_SLIP'',
    ''MONTHLY_PAY_SLIP_NOTICE'',

    coalesce(count(distinct dr.work_date), 0),
    coalesce(sum(dr.overtime_hours), 0),
    coalesce(sum(dr.night_work_hours), 0),

    coalesce(sum(dr.estimated_gross_pay_amount), 0),
    coalesce(sum(dr.overtime_hours), 0) * 0,
    coalesce(sum(dr.night_work_hours), 0) * 0,
    coalesce(sum(dr.allowance_amount), 0),
    0,
    0,
    0,

    0,
    0,
    0,
    0,
    0,
    coalesce(sum(dr.estimated_gross_pay_amount), 0),
    0,
    coalesce(epp.resident_tax_monthly, 0),
    coalesce(sum(dr.deduction_amount), 0),

    coalesce(sum(dr.estimated_gross_pay_amount), 0),
    coalesce(sum(dr.deduction_amount), 0)
        + coalesce(sum(dr.saving_amount), 0)
        + coalesce(sum(dr.loan_repayment_amount), 0)
        + coalesce(epp.resident_tax_monthly, 0),
    coalesce(sum(dr.estimated_gross_pay_amount), 0)
        - coalesce(sum(dr.deduction_amount), 0)
        - coalesce(sum(dr.saving_amount), 0)
        - coalesce(sum(dr.loan_repayment_amount), 0)
        - coalesce(epp.resident_tax_monthly, 0)
from monthly_pay_slip_input i
join employee e
  on e.tenant_id = i.tenant_id
 and e.deleted_at is null
left join employee_payroll_profile epp
  on epp.tenant_id = i.tenant_id
 and epp.employee_id = e.id
 and epp.deleted_at is null
left join daily_report dr
  on dr.tenant_id = i.tenant_id
 and dr.employee_id = e.id
 and dr.deleted_at is null
 and date_format(dr.work_date, ''%Y-%m'') = i.target_month
where i.execution_id = :executionId
  and i.deleted_at is null
  and (i.employee_id is null or i.employee_id = e.id)
group by
    i.tenant_id,
    i.execution_id,
    i.target_month,
    e.id,
    e.employee_code,
    e.employee_name,
    e.email,
    epp.resident_tax_monthly
order by e.employee_code;
',
    null,
    'reports/monthly',
    null,
    'SQL',
    'delete from monthly_pay_slip_output where execution_id = :executionId',
    null,
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
    pre_process_type,
    pre_process_sql,
    procedure_name,
    output_path,
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
    'SQL',
    '
delete from daily_pay_slip_output
where execution_id = :executionId;

insert into daily_pay_slip_output (
    tenant_id,
    created_at,
    updated_at,
    execution_id,
    payment_date,
    employee_id,
    employee_code,
    employee_name,
    recipient_key,
    recipient_name,
    recipient_email,
    business_key,
    mail_type,
    mail_template_key,
    labor_period_from,
    labor_period_to,
    basic_salary,
    overtime_allowance,
    night_allowance,
    attendance_allowance,
    driver_allowance,
    manager_allowance,
    other_allowance,
    legal_withholding_amount,
    legal_withholding_balance,
    gross_amount,
    deduction_total,
    net_payment_amount
)
select
    i.tenant_id,
    now(),
    now(),
    i.execution_id,
    i.payment_date,
    e.id,
    e.employee_code,
    e.employee_name,
    cast(e.id as char),
    e.employee_name,
    e.email,
    concat(
        ''DAILY_PAY_SLIP:'',
        date_format(i.payment_date, ''%Y-%m-%d''),
        '':'',
        e.id
    ),
    ''DAILY_PAY_SLIP'',
    ''DAILY_PAY_SLIP_NOTICE'',

    i.payment_date,
    i.payment_date,

    coalesce(dr.estimated_gross_pay_amount, dp.planned_amount, 0),
    0,
    0,
    coalesce(dr.allowance_amount, 0),
    0,
    0,
    0,

    coalesce(dr.deduction_amount, 0),
    0,

    coalesce(dr.estimated_gross_pay_amount, dp.planned_amount, 0),
    coalesce(dr.deduction_amount, 0),
    coalesce(dp.actual_amount, 0)
from daily_pay_slip_input i
join daily_payments dp
  on dp.tenant_id = i.tenant_id
 and dp.payment_date = i.payment_date
 and dp.deleted_at is null
join employee e
  on e.tenant_id = i.tenant_id
 and e.id = dp.employee_id
 and e.deleted_at is null
left join daily_report dr
  on dr.tenant_id = i.tenant_id
 and dr.employee_id = e.id
 and dr.payment_date = i.payment_date
 and dr.deleted_at is null
where i.execution_id = :executionId
  and i.deleted_at is null
  and (i.employee_id is null or i.employee_id = e.id)
order by e.employee_code;
',
    null,
    'reports/daily',
    null,
    'SQL',
    'delete from daily_pay_slip_output where execution_id = :executionId',
    null,
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
