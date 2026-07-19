create table if not exists daily_work_order_input (
    id bigint auto_increment primary key,
    tenant_id varchar(100) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6) null,

    execution_id varchar(100) not null,
    target_date date not null,

    index idx_daily_work_order_input_execution (execution_id),
    index idx_daily_work_order_input_target (tenant_id, target_date)
);

create table if not exists daily_work_order_output (
    id bigint auto_increment primary key,
    tenant_id varchar(100) not null,
    created_at datetime(6) null,
    updated_at datetime(6) null,
    deleted_at datetime(6) null,

    execution_id varchar(100) not null,

    target_date date null,

    employee_id bigint null,
    employee_code varchar(100) null,
    employee_name varchar(200) null,

    customer_id bigint null,
    customer_name varchar(255) null,

    customer_site_id bigint null,
    site_name varchar(255) null,

    work_description varchar(1000) null,

    index idx_daily_work_order_output_execution (execution_id),
    index idx_daily_work_order_output_target (tenant_id, target_date)
);