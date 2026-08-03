-- =====================================================
-- 日次支払明細 View -> Stored Procedure -> Output
-- =====================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS sp_daily_pay_slip_prepare;

DELIMITER $$

CREATE PROCEDURE sp_daily_pay_slip_prepare(
    IN p_execution_id VARCHAR(100)
)
BEGIN
    DECLARE v_tenant_id VARCHAR(100);
    DECLARE v_payment_date DATE;
    DECLARE v_employee_id BIGINT;

    SELECT input.tenant_id, input.payment_date, input.employee_id
    INTO v_tenant_id, v_payment_date, v_employee_id
    FROM daily_pay_slip_input input
    WHERE input.execution_id = p_execution_id
      AND input.deleted_at IS NULL
    ORDER BY input.id DESC
    LIMIT 1;

    IF v_tenant_id IS NULL OR v_payment_date IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'daily pay slip input is missing';
    END IF;

    DELETE FROM daily_pay_slip_output
    WHERE execution_id = p_execution_id;

    INSERT INTO daily_pay_slip_output (
        tenant_id, created_at, updated_at, execution_id, payment_date,
        employee_id, employee_code, employee_name,
        recipient_key, recipient_name, recipient_email,
        business_key, mail_type, mail_template_key,
        work_date, labor_period_from, labor_period_to,
        work_hours, overtime_hours, night_work_hours,
        basic_salary,
        allowance_item_name1, allowance_item_value1,
        allowance_item_name2, allowance_item_value2,
        allowance_item_name3, allowance_item_value3,
        allowance_item_name4, allowance_item_value4,
        allowance_item_name5, allowance_item_value5,
        allowance_item_name6, allowance_item_value6,
        allowance_item_name7, allowance_item_value7,
        allowance_item_name8, allowance_item_value8,
        allowance_item_name9, allowance_item_value9,
        allowance_item_name10, allowance_item_value10,
        deduction_item_name1, deduction_item_value1,
        deduction_item_name2, deduction_item_value2,
        deduction_item_name3, deduction_item_value3,
        deduction_item_name4, deduction_item_value4,
        deduction_item_name5, deduction_item_value5,
        deduction_item_name6, deduction_item_value6,
        deduction_item_name7, deduction_item_value7,
        deduction_item_name8, deduction_item_value8,
        deduction_item_name9, deduction_item_value9,
        deduction_item_name10, deduction_item_value10,
        gross_amount, allowance_total, deduction_total,
        daily_payment_amount, net_payment_amount, note
    )
    SELECT
        source.tenant_id,
        NOW(6),
        NOW(6),
        p_execution_id,
        source.payment_date,
        source.employee_id,
        source.employee_code,
        source.employee_name,
        CAST(source.employee_id AS CHAR),
        source.employee_name,
        source.recipient_email,
        CONCAT(
            'DAILY_PAY_SLIP:',
            DATE_FORMAT(source.payment_date, '%Y-%m-%d'),
            ':',
            source.employee_id
        ),
        'DAILY_PAY_SLIP',
        'DAILY_PAY_SLIP_NOTICE',
        source.payment_date,
        source.labor_period_from,
        source.labor_period_to,
        source.work_hours,
        source.overtime_hours,
        source.night_work_hours,
        source.basic_salary,
        source.allowance_item_name1, source.allowance_item_value1,
        source.allowance_item_name2, source.allowance_item_value2,
        source.allowance_item_name3, source.allowance_item_value3,
        source.allowance_item_name4, source.allowance_item_value4,
        source.allowance_item_name5, source.allowance_item_value5,
        source.allowance_item_name6, source.allowance_item_value6,
        source.allowance_item_name7, source.allowance_item_value7,
        source.allowance_item_name8, source.allowance_item_value8,
        source.allowance_item_name9, source.allowance_item_value9,
        source.allowance_item_name10, source.allowance_item_value10,
        source.deduction_item_name1, source.deduction_item_value1,
        source.deduction_item_name2, source.deduction_item_value2,
        source.deduction_item_name3, source.deduction_item_value3,
        source.deduction_item_name4, source.deduction_item_value4,
        source.deduction_item_name5, source.deduction_item_value5,
        source.deduction_item_name6, source.deduction_item_value6,
        source.deduction_item_name7, source.deduction_item_value7,
        source.deduction_item_name8, source.deduction_item_value8,
        source.deduction_item_name9, source.deduction_item_value9,
        source.deduction_item_name10, source.deduction_item_value10,
        source.gross_amount,
        source.allowance_total,
        source.deduction_total,
        source.daily_payment_amount,
        source.net_payment_amount,
        source.note
    FROM vw_daily_pay_slip_latest source
    WHERE source.tenant_id = v_tenant_id
      AND source.payment_date = v_payment_date
      AND (v_employee_id IS NULL OR source.employee_id = v_employee_id)
    ORDER BY source.employee_code;
END$$

DELIMITER ;

DROP PROCEDURE IF EXISTS sp_daily_pay_slip_cleanup;

DELIMITER $$

CREATE PROCEDURE sp_daily_pay_slip_cleanup(
    IN p_execution_id VARCHAR(100)
)
BEGIN
    DELETE FROM daily_pay_slip_output
    WHERE execution_id = p_execution_id;

    DELETE FROM daily_pay_slip_input
    WHERE execution_id = p_execution_id;
END$$

DELIMITER ;
