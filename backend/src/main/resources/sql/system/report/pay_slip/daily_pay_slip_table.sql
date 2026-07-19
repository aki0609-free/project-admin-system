drop table if exists daily_pay_slip_output;

create table daily_pay_slip_output (
    id bigint auto_increment primary key,

    tenant_id varchar(100) not null,
    created_at datetime(6) null,
    updated_at datetime(6) null,
    deleted_at datetime(6) null,

    execution_id varchar(100) not null,
    payment_date date not null,

    employee_id bigint not null,
    employee_code varchar(100) null,
    employee_name varchar(200) null,
    recipient_key varchar(255) null,
    recipient_name varchar(255) null,
    recipient_email varchar(255) null,
    business_key varchar(255) null,
    mail_type varchar(100) null,
    mail_template_key varchar(100) null,

    work_date date null,
    labor_period_from date null,
    labor_period_to date null,

    work_hours decimal(10,2) null,
    overtime_hours decimal(10,2) null,
    night_work_hours decimal(10,2) null,

    basic_salary decimal(15,2) null,

    allowance_item_name1 varchar(100) null,
    allowance_item_value1 decimal(15,2) null,
    allowance_item_name2 varchar(100) null,
    allowance_item_value2 decimal(15,2) null,
    allowance_item_name3 varchar(100) null,
    allowance_item_value3 decimal(15,2) null,
    allowance_item_name4 varchar(100) null,
    allowance_item_value4 decimal(15,2) null,
    allowance_item_name5 varchar(100) null,
    allowance_item_value5 decimal(15,2) null,
    allowance_item_name6 varchar(100) null,
    allowance_item_value6 decimal(15,2) null,
    allowance_item_name7 varchar(100) null,
    allowance_item_value7 decimal(15,2) null,
    allowance_item_name8 varchar(100) null,
    allowance_item_value8 decimal(15,2) null,
    allowance_item_name9 varchar(100) null,
    allowance_item_value9 decimal(15,2) null,
    allowance_item_name10 varchar(100) null,
    allowance_item_value10 decimal(15,2) null,

    deduction_item_name1 varchar(100) null,
    deduction_item_value1 decimal(15,2) null,
    deduction_item_name2 varchar(100) null,
    deduction_item_value2 decimal(15,2) null,
    deduction_item_name3 varchar(100) null,
    deduction_item_value3 decimal(15,2) null,
    deduction_item_name4 varchar(100) null,
    deduction_item_value4 decimal(15,2) null,
    deduction_item_name5 varchar(100) null,
    deduction_item_value5 decimal(15,2) null,
    deduction_item_name6 varchar(100) null,
    deduction_item_value6 decimal(15,2) null,
    deduction_item_name7 varchar(100) null,
    deduction_item_value7 decimal(15,2) null,
    deduction_item_name8 varchar(100) null,
    deduction_item_value8 decimal(15,2) null,
    deduction_item_name9 varchar(100) null,
    deduction_item_value9 decimal(15,2) null,
    deduction_item_name10 varchar(100) null,
    deduction_item_value10 decimal(15,2) null,

    gross_amount decimal(15,2) null,
    allowance_total decimal(15,2) null,
    deduction_total decimal(15,2) null,
    daily_payment_amount decimal(15,2) null,
    net_payment_amount decimal(15,2) null,

    note varchar(1000) null,

    index idx_daily_pay_slip_output_execution (execution_id),
    index idx_daily_pay_slip_output_delivery (execution_id, business_key),
    index idx_daily_pay_slip_output_employee (tenant_id, payment_date, employee_id)
);

drop table if exists daily_pay_slip_input;

create table daily_pay_slip_input (
    id bigint auto_increment primary key,

    tenant_id varchar(100) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6) null,

    execution_id varchar(100) not null,
    payment_date date not null,
    employee_id bigint null,

    index idx_daily_pay_slip_input_execution (execution_id),
    index idx_daily_pay_slip_input_target (tenant_id, payment_date)
);
