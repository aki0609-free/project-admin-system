create or replace view vw_monthly_invoice as
select
    tenant_id,

    target_month,

    history_version,

    customer_id,
    customer_name,

    customer_site_id,
    site_name,

    work_report_count,
    worker_count,

    total_work_hours,
    total_overtime_hours,
    total_night_work_hours,

    unit_price,
    billing_amount

from monthly_invoice_history

where deleted_at is null;