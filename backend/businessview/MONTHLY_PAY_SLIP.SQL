create or replace view vw_monthly_pay_slip as
select
    h.tenant_id,

    h.target_month,

    h.history_version,

    h.employee_id,
    h.employee_code,
    h.employee_name,

    h.work_day_count,

    h.total_work_hours,
    h.total_overtime_hours,
    h.total_night_work_hours,

    h.gross_amount,

    h.total_allowance_amount,
    h.total_deduction_amount,

    h.daily_payment_amount,

    h.saving_amount,
    h.loan_repayment_amount,

    h.net_payment_amount,

    (
        select json_arrayagg(
            json_object(
                'type', i.item_type,
                'code', i.item_code,
                'name', i.item_name,
                'amount', i.amount
            )
        )
        from monthly_pay_slip_history_item i
        where i.pay_slip_history_id = h.id
        and i.deleted_at is null
    ) items_json

from monthly_pay_slip_history h

where h.deleted_at is null;