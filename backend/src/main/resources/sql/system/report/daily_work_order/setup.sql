set @tenant_id = 'default';
set @now = now();

delete rp
from report_param rp
join report_master rm
  on rm.id = rp.report_master_id
where rm.tenant_id = @tenant_id
  and rm.report_code = 'DAILY_WORK_ORDER';

delete from report_master
where tenant_id = @tenant_id
  and report_code = 'DAILY_WORK_ORDER';

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
    'DAILY_WORK_ORDER',
    '作業証明伝票',
    'DailyWorkOrder.jrxml',
    'daily_work_order',
    'daily_work_order_input',
    'daily_work_order_output',
    'SQL',
    '
insert into daily_work_order_output (
    execution_id,
    tenant_id,
    target_date,
    employee_id,
    employee_code,
    employee_name,
    customer_id,
    customer_name,
    customer_site_id,
    site_name,
    work_description
)
select
    :executionId,
    v.tenant_id,
    v.target_date,
    v.employee_id,
    v.employee_code,
    v.employee_name,
    v.customer_id,
    v.customer_name,
    v.customer_site_id,
    v.site_name,
    v.work_description
from vw_daily_work_order v
join daily_work_order_input i
  on i.execution_id = :executionId
 and i.tenant_id = v.tenant_id
 and i.target_date = v.target_date
where v.tenant_id = i.tenant_id
  and v.target_date = i.target_date
',
    null,
    'reports/daily',
    null,
    'SQL',
    'delete from daily_work_order_output where execution_id = :executionId',
    null,
    'SINGLE',
    1,
    '作業証明伝票_${targetDate}',
    'PDF',
    false,
    true,
    true
);

set @report_master_id = (
    select id
    from report_master
    where tenant_id = @tenant_id
      and report_code = 'DAILY_WORK_ORDER'
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
    @report_master_id,
    'targetDate',
    '対象日',
    'DATE',
    'DATE',
    true,
    true,
    false,
    true,
    null,
    '対象日を選択',
    'target_date',
    1,
    true
);
