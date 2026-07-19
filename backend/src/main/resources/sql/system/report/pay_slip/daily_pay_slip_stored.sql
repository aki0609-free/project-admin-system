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

    work_date,

    work_hours,
    overtime_hours,
    night_work_hours,

    basic_salary,

    gross_amount,
    allowance_total,
    deduction_total,
    daily_payment_amount,
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
        'DAILY_PAY_SLIP:',
        date_format(i.payment_date, '%Y-%m-%d'),
        ':',
        e.id
    ),
    'DAILY_PAY_SLIP',
    'DAILY_PAY_SLIP_NOTICE',

    i.payment_date,

    8,
    0,
    0,

    10000,

    10000,
    0,
    0,
    10000,
    10000

from daily_pay_slip_input i

join employee e
  on e.tenant_id = i.tenant_id
 and e.deleted_at is null

where i.execution_id = :executionId
and (
    i.employee_id is null
    or i.employee_id = e.id
);
