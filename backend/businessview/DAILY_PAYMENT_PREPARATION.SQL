create or replace view vw_daily_payment_preparation as
select
    dp.tenant_id,
    dp.payment_date,

    dp.employee_id,
    dp.employee_code,
    dp.employee_name,

    dp.planned_amount,
    dp.actual_amount,

    dp.status,
    dp.note

from daily_payments dp

where dp.deleted_at is null;