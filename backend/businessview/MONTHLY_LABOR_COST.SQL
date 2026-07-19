create or replace view vw_monthly_labor_cost as
select
    tenant_id,

    target_month,

    history_version,

    employee_id,
    employee_code,
    employee_name,

    work_day_count,

    total_work_hours,
    total_overtime_hours,
    total_night_work_hours,

    allowance_amount,
    deduction_amount,

    saving_amount,
    loan_repayment_amount,

    gross_amount,
    daily_payment_amount,

    net_payment_amount

from monthly_labor_cost_history

where deleted_at is null;