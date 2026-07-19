create or replace view vw_monthly_payment_confirm as
select
    tenant_id,

    target_month,

    history_version,

    customer_id,
    customer_name,

    customer_site_id,
    site_name,

    invoice_amount,
    paid_amount,
    unpaid_amount,

    payment_due_date,
    paid_date,

    status,
    note

from monthly_payment_confirm_history

where deleted_at is null;