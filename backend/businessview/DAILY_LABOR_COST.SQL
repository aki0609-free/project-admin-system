create or replace view vw_daily_labor_cost as
select
    dr.tenant_id,
    dr.work_date as target_date,

    e.id employee_id,
    e.employee_code,
    e.employee_name,

    dr.customer_name,
    dr.site_name,

    dr.work_hours,
    dr.overtime_hours,
    dr.night_work_hours,

    dr.estimated_gross_pay_amount labor_cost

from daily_report dr

join employee e
    on e.id = dr.employee_id

where dr.deleted_at is null;