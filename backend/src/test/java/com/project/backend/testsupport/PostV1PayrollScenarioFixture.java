package com.project.backend.testsupport;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * V1後の給与回帰テストで共有する、日払い・週払い・月払いの固定シナリオ。
 *
 * <p>金額と日付を一か所に集約し、締め・帳票・給与支払表のテスト間で
 * 期待値がずれないようにする。</p>
 */
public final class PostV1PayrollScenarioFixture {

    public static final LocalDate TARGET_MONTH = LocalDate.of(2026, 7, 1);
    public static final LocalDate DAILY_PAYMENT_DATE = LocalDate.of(2026, 7, 4);
    public static final LocalDate WEEKLY_PAYMENT_DATE = LocalDate.of(2026, 7, 13);
    public static final LocalDate MONTHLY_PAYMENT_DATE = LocalDate.of(2026, 8, 14);

    private final JdbcTemplate jdbcTemplate;
    private final String tenantId;

    public PostV1PayrollScenarioFixture(
            JdbcTemplate jdbcTemplate,
            String tenantId
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantId = tenantId;
    }

    public Scenario create() {
        ensureCompanyProfile();
        ensurePayrollSetting();
        ensureCalculationPeriod();
        ensureMonthlyClosing();

        long dailyEmployeeId = insertEmployee(
                "POSTV1-DAILY-001", "日払い回帰社員", "DAILY", "12000"
        );
        long weeklyEmployeeId = insertEmployee(
                "POSTV1-WEEKLY-001", "週払い回帰社員", "WEEKLY", "13000"
        );
        long monthlyEmployeeId = insertEmployee(
                "POSTV1-MONTHLY-001", "月払い回帰社員", "MONTHLY", "14000"
        );

        insertReport(
                dailyEmployeeId,
                LocalDate.of(2026, 7, 3),
                DAILY_PAYMENT_DATE,
                "12000"
        );
        insertReport(
                weeklyEmployeeId,
                LocalDate.of(2026, 7, 6),
                WEEKLY_PAYMENT_DATE,
                "13000"
        );
        insertReport(
                weeklyEmployeeId,
                LocalDate.of(2026, 7, 10),
                WEEKLY_PAYMENT_DATE,
                "13000"
        );
        insertReport(
                monthlyEmployeeId,
                LocalDate.of(2026, 7, 7),
                MONTHLY_PAYMENT_DATE,
                "14000"
        );
        insertReport(
                monthlyEmployeeId,
                LocalDate.of(2026, 7, 20),
                MONTHLY_PAYMENT_DATE,
                "14000"
        );

        return new Scenario(
                dailyEmployeeId,
                weeklyEmployeeId,
                monthlyEmployeeId
        );
    }

    private void ensureCompanyProfile() {
        jdbcTemplate.update("""
                INSERT INTO company_profile (
                    company_code, company_name, active_flag,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    'POSTV1-IT', 'V1後回帰テスト会社', TRUE,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, tenantId);
    }

    private void ensurePayrollSetting() {
        jdbcTemplate.update("""
                INSERT INTO closing_setting (
                    setting_code,
                    closing_day_type, closing_day_value, closing_month_offset,
                    payment_day_type, payment_day_value, payment_month_offset,
                    active_flag, tenant_id, created_at, updated_at
                ) VALUES (
                    'PAYROLL',
                    'END_OF_MONTH', NULL, 0,
                    'DAY_OF_MONTH', 15, 1,
                    TRUE, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, tenantId);
    }

    private void ensureCalculationPeriod() {
        jdbcTemplate.update("""
                INSERT INTO payroll_calculation_period (
                    target_month, income_tax_year, insurance_rate_year,
                    child_care_support_required, rounding_mode,
                    verified_flag, verified_at, verified_by, source_note,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    '2026-07-01', 2026, 2026,
                    FALSE, 'HALF_UP',
                    TRUE, CURRENT_TIMESTAMP(6), 'post-v1-integration',
                    '日払い・週払い・月払い共通回帰fixture',
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, tenantId);
    }

    private void ensureMonthlyClosing() {
        jdbcTemplate.update("""
                INSERT INTO monthly_closings (
                    target_month, closing_start_date, closing_end_date,
                    status, closing_version, note,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    '2026-07-01', '2026-07-01', '2026-07-31',
                    'OPEN', 0, 'V1後主要業務フロー回帰fixture',
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, tenantId);
    }

    private long insertEmployee(
            String employeeCode,
            String employeeName,
            String paymentCycle,
            String dailyWage
    ) {
        jdbcTemplate.update("""
                INSERT INTO employee (
                    employee_code, employee_name, employment_type,
                    employment_status, active_flag, dormitory_flag,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, ?, 'FULL_TIME',
                    'ACTIVE', TRUE, FALSE,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeCode, employeeName, tenantId);
        Long employeeId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM employee
                WHERE tenant_id = ? AND employee_code = ?
                """, Long.class, tenantId, employeeCode);
        if (employeeId == null) {
            throw new IllegalStateException("fixture employee was not created");
        }

        jdbcTemplate.update("""
                INSERT INTO employee_contract (
                    employee_id, contract_start_date, renewal_flag,
                    salary_type, payment_cycle,
                    monthly_salary, weekly_wage, daily_wage, hourly_wage,
                    standard_working_hours,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, '2026-04-01', FALSE,
                    'DAILY', ?,
                    0, 0, ?, 0,
                    8,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeId, paymentCycle, dailyWage, tenantId);

        jdbcTemplate.update("""
                INSERT INTO employee_payroll_profile (
                    employee_id, tax_category, tax_dependent_count,
                    dependent_flag, dependent_of_other_flag,
                    paid_leave_remaining_days,
                    income_tax_calc_flag, resident_tax_calc_flag,
                    resident_tax_monthly,
                    employment_insurance_flag, social_insurance_flag,
                    health_insurance_flag, pension_insurance_flag,
                    care_insurance_flag, daily_pay_flag,
                    commute_allowance_monthly,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, 'KOU', 0,
                    FALSE, FALSE, 0,
                    FALSE, FALSE, 0,
                    FALSE, FALSE,
                    FALSE, FALSE,
                    FALSE, FALSE, 0,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeId, tenantId);
        return employeeId;
    }

    private void insertReport(
            long employeeId,
            LocalDate workDate,
            LocalDate paymentDate,
            String grossAmount
    ) {
        jdbcTemplate.update("""
                INSERT INTO daily_report (
                    employee_id, work_date, payment_date,
                    normal_pay_amount, allowance_amount, deduction_amount,
                    saving_amount, saving_withdrawal_amount,
                    loan_repayment_amount,
                    estimated_gross_pay_amount, estimated_net_pay_amount,
                    billing_base_unit_price, billing_overtime_unit_price,
                    billing_night_unit_price, billing_holiday_unit_price,
                    billing_commute_unit_price, holiday_premium_eligible,
                    dormitory_charge_days, overtime_pay_amount,
                    night_pay_amount, holiday_pay_amount,
                    work_hours, vehicle_used_flag,
                    work_description, approval_status,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, ?, ?,
                    ?, 0, 0,
                    0, 0, 0,
                    ?, ?,
                    0, 0, 0, 0, 0, FALSE,
                    0, 0, 0, 0,
                    8, FALSE,
                    'V1後主要業務フロー回帰fixture', 'APPROVED',
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """,
                employeeId, workDate, paymentDate,
                grossAmount, grossAmount, grossAmount, tenantId);
    }

    public record Scenario(
            long dailyEmployeeId,
            long weeklyEmployeeId,
            long monthlyEmployeeId
    ) {
    }
}
