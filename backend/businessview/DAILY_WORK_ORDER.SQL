create or replace view vw_daily_work_order as
select
    dp.tenant_id,

    dp.target_date,

    a.employee_id,
    a.employee_code,
    a.employee_name,

    a.customer_id,
    a.customer_name,

    a.customer_site_id,
    a.site_name,

    a.work_description

from daily_preparations dp

join daily_preparation_assignments a
    on a.preparation_id = dp.id

where dp.deleted_at is null
and a.deleted_at is null;