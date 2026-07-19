create or replace view vw_daily_pay_slip as
select
    dr.tenant_id,

    dr.payment_date,

    e.id employee_id,
    e.employee_code,
    e.employee_name,

    dr.estimated_gross_pay_amount gross_amount,
    dr.estimated_net_pay_amount net_amount,

    (
        select json_arrayagg(
            json_object(
                'code', a.allowance_code,
                'name', a.allowance_name,
                'amount', a.amount
            )
        )
        from daily_report_allowances a
        where a.daily_report_id = dr.id
        and a.deleted_at is null
    ) allowances_json,

    (
        select json_arrayagg(
            json_object(
                'code', d.deduction_code,
                'name', d.deduction_name,
                'amount', d.amount
            )
        )
        from daily_report_deductions d
        where d.daily_report_id = dr.id
        and d.deleted_at is null
    ) deductions_json

from daily_report dr

join employee e
    on e.id = dr.employee_id

where dr.deleted_at is null;