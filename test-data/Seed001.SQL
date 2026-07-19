-- =====================================================
-- DEV SEED: operation daily / preparation
-- tenant_id = default
-- =====================================================

set @tenant_id = 'default';
set @now = now();

-- 既存テストデータ削除
delete from daily_preparation_dispatches where tenant_id = @tenant_id;
delete from daily_preparation_assignments where tenant_id = @tenant_id;
delete from daily_preparations where tenant_id = @tenant_id;

delete from daily_payments where tenant_id = @tenant_id;
delete from daily_report_deductions where tenant_id = @tenant_id;
delete from daily_report_allowances where tenant_id = @tenant_id;
delete from daily_report where tenant_id = @tenant_id;


-- =====================================================
-- 従業員
-- =====================================================

insert into employee (
    tenant_id,
    created_at,
    updated_at,
    employee_code,
    employee_name,
    employee_name_kana,
    gender,
    hire_date,
    employment_type,
    employment_status,
    phone,
    active_flag
) values
(@tenant_id, @now, @now, 'EMP101', '鈴木 一郎', 'スズキ イチロウ', 'MALE', '2024-04-01', 'FULL_TIME', 'ACTIVE', '090-0000-0001', true),
(@tenant_id, @now, @now, 'EMP102', '山田 太郎', 'ヤマダ タロウ', 'MALE', '2024-04-01', 'FULL_TIME', 'ACTIVE', '090-0000-0002', true),
(@tenant_id, @now, @now, 'EMP103', '佐藤 健', 'サトウ ケン', 'MALE', '2024-04-01', 'PART_TIME', 'ACTIVE', '090-0000-0003', true),
(@tenant_id, @now, @now, 'EMP104', '高橋 誠', 'タカハシ マコト', 'MALE', '2024-04-01', 'FULL_TIME', 'ACTIVE', '090-0000-0004', true),
(@tenant_id, @now, @now, 'EMP105', '伊藤 翔', 'イトウ ショウ', 'MALE', '2024-04-01', 'PART_TIME', 'ACTIVE', '090-0000-0005', true);

insert into employee_contract (
    tenant_id,
    created_at,
    updated_at,
    employee_id,
    contract_start_date,
    salary_type,
    daily_wage,
    hourly_wage,
    standard_working_hours,
    renewal_flag
)
select
    @tenant_id,
    @now,
    @now,
    id,
    '2024-04-01',
    'DAILY',
    11000,
    1375,
    8.00,
    true
from employee
where tenant_id = @tenant_id;

insert into employee_payroll_profile (
    tenant_id,
    created_at,
    updated_at,
    employee_id,
    tax_category,
    tax_dependent_count,
    income_tax_calc_flag,
    resident_tax_calc_flag,
    resident_tax_monthly,
    employment_insurance_flag,
    social_insurance_flag,
    health_insurance_flag,
    pension_insurance_flag,
    care_insurance_flag,
    daily_pay_flag,
    commute_allowance_monthly
)
select
    @tenant_id,
    @now,
    @now,
    id,
    'KOU',
    0,
    true,
    true,
    0,
    true,
    true,
    true,
    true,
    false,
    true,
    0
from employee
where tenant_id = @tenant_id;

-- =====================================================
-- 顧客・現場
-- =====================================================

insert into customers (
    tenant_id,
    created_at,
    updated_at,
    name,
    furigana_name,
    short_name,
    post_no,
    address,
    representative_name,
    phone,
    job_type,
    contract_flag
) values
(@tenant_id, @now, @now, '池ノ沢工業', 'イケノサワコウギョウ', '池ノ沢', '417-0000', '静岡県富士市テスト1-1', '池田 一郎', '0545-00-0001', '建設', 'ACTIVE'),
(@tenant_id, @now, @now, '光塗装', 'ヒカリトソウ', '光塗装', '418-0000', '静岡県富士宮市テスト2-1', '光 太郎', '0544-00-0002', '塗装', 'ACTIVE');

insert into customer_sites (
    tenant_id,
    created_at,
    updated_at,
    customer_id,
    name,
    base_unit_price,
    contact_person_name,
    contact_person_phone,
    distance_from_company_km
)
select
    @tenant_id,
    @now,
    @now,
    c.id,
    '柳沢第3橋',
    18000,
    '現場 太郎',
    '090-1111-1111',
    18
from customers c
where c.tenant_id = @tenant_id
  and c.name = '池ノ沢工業';

insert into customer_sites (
    tenant_id,
    created_at,
    updated_at,
    customer_id,
    name,
    base_unit_price,
    contact_person_name,
    contact_person_phone,
    distance_from_company_km
)
select
    @tenant_id,
    @now,
    @now,
    c.id,
    '富士高架橋塗装',
    20000,
    '現場 次郎',
    '090-2222-2222',
    12
from customers c
where c.tenant_id = @tenant_id
  and c.name = '光塗装';

-- =====================================================
-- 日報
-- =====================================================

insert into daily_report (
    tenant_id,
    created_at,
    updated_at,
    employee_id,
    work_date,
    payment_date,
    customer_id,
    customer_site_id,
    customer_name,
    site_name,
    work_description,
    start_time,
    end_time,
    break_minutes,
    work_hours,
    overtime_hours,
    night_work_hours,
    allowance_amount,
    deduction_amount,
    loan_repayment_amount,
    saving_amount,
    estimated_gross_pay_amount,
    estimated_net_pay_amount,
    vehicle_used_flag,
    mileage,
    paid_leave_used_flag,
    approval_status
)
select
    @tenant_id,
    @now,
    @now,
    e.id,
    '2026-06-12',
    '2026-06-12',
    c.id,
    s.id,
    c.name,
    s.name,
    '橋梁補修工事 足場組立・ケレン作業',
    '08:00',
    '17:00',
    60,
    8.00,
    1.00,
    0.00,
    1000,
    500,
    0,
    1000,
    14000,
    11500,
    true,
    18,
    false,
    'APPROVED'
from employee e
join customers c on c.tenant_id = @tenant_id and c.name = '池ノ沢工業'
join customer_sites s on s.tenant_id = @tenant_id and s.customer_id = c.id and s.name = '柳沢第3橋'
where e.tenant_id = @tenant_id
  and e.employee_code in ('EMP001', 'EMP002', 'EMP003');

insert into daily_report (
    tenant_id,
    created_at,
    updated_at,
    employee_id,
    work_date,
    payment_date,
    customer_id,
    customer_site_id,
    customer_name,
    site_name,
    work_description,
    start_time,
    end_time,
    break_minutes,
    work_hours,
    overtime_hours,
    night_work_hours,
    allowance_amount,
    deduction_amount,
    loan_repayment_amount,
    saving_amount,
    estimated_gross_pay_amount,
    estimated_net_pay_amount,
    vehicle_used_flag,
    mileage,
    paid_leave_used_flag,
    approval_status
)
select
    @tenant_id,
    @now,
    @now,
    e.id,
    '2026-06-12',
    '2026-06-12',
    c.id,
    s.id,
    c.name,
    s.name,
    '橋梁塗装工事 下地処理・中塗り',
    '08:30',
    '17:30',
    60,
    8.00,
    2.00,
    0.00,
    1500,
    500,
    0,
    0,
    15000,
    14000,
    true,
    12,
    false,
    'APPROVED'
from employee e
join customers c on c.tenant_id = @tenant_id and c.name = '光塗装'
join customer_sites s on s.tenant_id = @tenant_id and s.customer_id = c.id and s.name = '富士高架橋塗装'
where e.tenant_id = @tenant_id
  and e.employee_code in ('EMP004', 'EMP005');

-- =====================================================
-- 日報手当・控除
-- =====================================================

insert into daily_report_allowances (
    tenant_id,
    created_at,
    updated_at,
    daily_report_id,
    allowance_master_id,
    allowance_code,
    allowance_name,
    amount
)
select
    @tenant_id,
    @now,
    @now,
    dr.id,
    1,
    'DRV',
    '運転手当',
    1000
from daily_report dr
where dr.tenant_id = @tenant_id
  and dr.work_date = '2026-06-12';

insert into daily_report_deductions (
    tenant_id,
    created_at,
    updated_at,
    daily_report_id,
    deduction_master_id,
    deduction_code,
    deduction_name,
    amount
)
select
    @tenant_id,
    @now,
    @now,
    dr.id,
    1,
    'DORM',
    '寮費',
    500
from daily_report dr
where dr.tenant_id = @tenant_id
  and dr.work_date = '2026-06-12';

-- =====================================================
-- 日払い
-- =====================================================

insert into daily_payments (
    tenant_id,
    created_at,
    updated_at,
    payment_date,
    employee_id,
    employee_code,
    employee_name,
    planned_amount,
    actual_amount,
    status,
    paid_at,
    note
)
select
    @tenant_id,
    @now,
    @now,
    '2026-06-12',
    e.id,
    e.employee_code,
    e.employee_name,
    case e.employee_code
        when 'EMP001' then 10000
        when 'EMP002' then 9000
        when 'EMP003' then 8000
        when 'EMP004' then 12000
        else 10000
    end,
    case e.employee_code
        when 'EMP001' then 10000
        when 'EMP002' then 8500
        when 'EMP003' then 8000
        when 'EMP004' then 12000
        else 9500
    end,
    'PENDING',
    null,
    null
from employee e
where e.tenant_id = @tenant_id;

-- =====================================================
-- 翌日準備
-- =====================================================

insert into daily_preparations (
    tenant_id,
    created_at,
    updated_at,
    target_date,
    status,
    note
) values
(@tenant_id, @now, @now, '2026-06-13', 'OPEN', 'dev seed');

set @preparation_id = (
    select id
    from daily_preparations
    where tenant_id = @tenant_id
      and target_date = '2026-06-13'
    limit 1
);

insert into daily_preparation_assignments (
    tenant_id,
    created_at,
    updated_at,
    preparation_id,
    employee_id,
    employee_code,
    employee_name,
    customer_id,
    customer_site_id,
    customer_name,
    site_name,
    work_description
)
select
    @tenant_id,
    @now,
    @now,
    @preparation_id,
    e.id,
    e.employee_code,
    e.employee_name,
    c.id,
    s.id,
    c.name,
    s.name,
    '橋梁補修工事 足場組立・ケレン作業'
from employee e
join customers c on c.tenant_id = @tenant_id and c.name = '池ノ沢工業'
join customer_sites s on s.tenant_id = @tenant_id and s.customer_id = c.id and s.name = '柳沢第3橋'
where e.tenant_id = @tenant_id
  and e.employee_code in ('EMP001', 'EMP002', 'EMP003');

insert into daily_preparation_assignments (
    tenant_id,
    created_at,
    updated_at,
    preparation_id,
    employee_id,
    employee_code,
    employee_name,
    customer_id,
    customer_site_id,
    customer_name,
    site_name,
    work_description
)
select
    @tenant_id,
    @now,
    @now,
    @preparation_id,
    e.id,
    e.employee_code,
    e.employee_name,
    c.id,
    s.id,
    c.name,
    s.name,
    '橋梁塗装工事 下地処理・中塗り'
from employee e
join customers c on c.tenant_id = @tenant_id and c.name = '光塗装'
join customer_sites s on s.tenant_id = @tenant_id and s.customer_id = c.id and s.name = '富士高架橋塗装'
where e.tenant_id = @tenant_id
  and e.employee_code in ('EMP004', 'EMP005');

insert into daily_preparation_dispatches (
    tenant_id,
    created_at,
    updated_at,
    preparation_id,
    customer_id,
    customer_site_id,
    customer_name,
    site_name,
    distance_from_company_km,
    vehicle_count,
    note
)
select
    @tenant_id,
    @now,
    @now,
    @preparation_id,
    c.id,
    s.id,
    c.name,
    s.name,
    s.distance_from_company_km,
    2,
    '普通車2台'
from customers c
join customer_sites s on s.tenant_id = @tenant_id and s.customer_id = c.id
where c.tenant_id = @tenant_id
  and c.name = '池ノ沢工業'
  and s.name = '柳沢第3橋';

insert into daily_preparation_dispatches (
    tenant_id,
    created_at,
    updated_at,
    preparation_id,
    customer_id,
    customer_site_id,
    customer_name,
    site_name,
    distance_from_company_km,
    vehicle_count,
    note
)
select
    @tenant_id,
    @now,
    @now,
    @preparation_id,
    c.id,
    s.id,
    c.name,
    s.name,
    s.distance_from_company_km,
    1,
    '軽バン1台'
from customers c
join customer_sites s on s.tenant_id = @tenant_id and s.customer_id = c.id
where c.tenant_id = @tenant_id
  and c.name = '光塗装'
  and s.name = '富士高架橋塗装';