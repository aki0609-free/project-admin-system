create or replace view vw_monthly_site_summary as
select
    tenant_id,

    target_month,

    history_version,

    customer_id,
    customer_name,

    customer_site_id,
    site_name,

    work_day,

    regular_count,
    regular_amount,

    overtime_hours,
    overtime_amount,

    night_work_hours,
    night_work_amount,

    other_hours,
    other_amount,

    total_amount

from monthly_site_summary_history

where deleted_at is null;