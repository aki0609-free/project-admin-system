package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.SalaryType;

import lombok.RequiredArgsConstructor;

/**
 * 日次の法定準備金に使う月額法定控除の概算値を算出する。
 *
 * <p>月次給与明細と同じ税・保険マスターを参照する。給与額だけは月の途中でも
 * 初期値を提示できるよう、契約上の給与計算基準から20勤務日相当へ換算する。</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyStatutoryDeductionEstimateService {

    private static final BigDecimal MONTHLY_WORK_DAYS = BigDecimal.valueOf(20);
    private static final BigDecimal WEEKS_PER_YEAR = BigDecimal.valueOf(52);
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BigDecimal estimateMonthlyTotal(
            Long employeeId,
            LocalDate targetDate,
            EmployeeContract contract
    ) {
        if (employeeId == null || targetDate == null || contract == null) {
            return BigDecimal.ZERO;
        }

        LocalDate targetMonth = targetDate.withDayOfMonth(1);
        String tenantId = TenantContext.getTenantId();
        Map<String, Object> params = Map.of(
                "tenantId", tenantId,
                "employeeId", employeeId,
                "targetDate", targetDate,
                "targetMonth", targetMonth,
                "fiscalYear", targetDate.getYear()
                        - (targetDate.getMonthValue() < 6 ? 1 : 0),
                "month", targetDate.getMonthValue()
        );

        CalculationBasis basis = findCalculationBasis(params);
        if (basis == null || !basis.verified()) {
            return BigDecimal.ZERO;
        }

        BigDecimal gross = projectedMonthlyGross(contract);
        BigDecimal health = BigDecimal.ZERO;
        BigDecimal childSupport = BigDecimal.ZERO;
        BigDecimal pension = BigDecimal.ZERO;
        BigDecimal employment = BigDecimal.ZERO;

        if (basis.socialInsurance()) {
            if (basis.healthInsurance()) {
                health = yen(basis.healthRemuneration()
                        .multiply(rate(basis.rates(), "HEALTH_INSURANCE")));
                if (basis.careInsurance()) {
                    health = health.add(yen(basis.healthRemuneration()
                            .multiply(rate(basis.rates(), "CARE_INSURANCE"))));
                }
                if (basis.childSupportRequired()) {
                    childSupport = yen(basis.healthRemuneration()
                            .multiply(rate(basis.rates(), "CHILD_CARE_SUPPORT")));
                }
            }
            if (basis.pensionInsurance()) {
                pension = yen(basis.pensionRemuneration()
                        .multiply(rate(basis.rates(), "PENSION")));
            }
        }
        if (basis.employmentInsurance()) {
            employment = yen(gross.multiply(
                    rate(basis.rates(), "EMPLOYMENT_INSURANCE")));
        }

        BigDecimal taxable = gross
                .subtract(health)
                .subtract(childSupport)
                .subtract(pension)
                .subtract(employment)
                .max(BigDecimal.ZERO);
        BigDecimal incomeTax = basis.incomeTax()
                ? findIncomeTax(
                        basis.incomeTaxYear(),
                        basis.taxDependentCount(),
                        taxable
                )
                : BigDecimal.ZERO;

        return incomeTax
                .add(basis.residentTax())
                .add(health)
                .add(childSupport)
                .add(pension)
                .add(employment);
    }

    private CalculationBasis findCalculationBasis(Map<String, Object> params) {
        String sql = """
                SELECT
                    period.income_tax_year,
                    period.insurance_rate_year,
                    period.child_care_support_required,
                    period.verified_flag,
                    COALESCE(profile.tax_dependent_count, 0) AS tax_dependent_count,
                    COALESCE(profile.income_tax_calc_flag, TRUE) AS income_tax_calc_flag,
                    COALESCE(profile.resident_tax_calc_flag, TRUE) AS resident_tax_calc_flag,
                    COALESCE(profile.employment_insurance_flag, TRUE) AS employment_insurance_flag,
                    COALESCE(profile.social_insurance_flag, TRUE) AS social_insurance_flag,
                    COALESCE(profile.health_insurance_flag, TRUE) AS health_insurance_flag,
                    COALESCE(profile.pension_insurance_flag, TRUE) AS pension_insurance_flag,
                    COALESCE(profile.care_insurance_flag, FALSE) AS care_insurance_flag,
                    COALESCE(resident.tax_amount, 0) AS resident_tax,
                    COALESCE(remuneration.health_standard_remuneration, 0)
                        AS health_standard_remuneration,
                    COALESCE(remuneration.pension_standard_remuneration, 0)
                        AS pension_standard_remuneration
                FROM payroll_calculation_period period
                LEFT JOIN employee_payroll_profile profile
                  ON profile.tenant_id = period.tenant_id
                 AND profile.employee_id = :employeeId
                 AND profile.deleted_at IS NULL
                LEFT JOIN resident_tax_monthly resident
                  ON resident.tenant_id = period.tenant_id
                 AND resident.employee_id = :employeeId
                 AND resident.fiscal_year = :fiscalYear
                 AND resident.month = :month
                 AND resident.deleted_at IS NULL
                LEFT JOIN employee_standard_remuneration remuneration
                  ON remuneration.id = (
                      SELECT candidate.id
                      FROM employee_standard_remuneration candidate
                      WHERE candidate.tenant_id = period.tenant_id
                        AND candidate.employee_id = :employeeId
                        AND candidate.deleted_at IS NULL
                        AND candidate.effective_from <= :targetDate
                        AND (candidate.effective_to IS NULL
                             OR candidate.effective_to >= :targetDate)
                      ORDER BY candidate.effective_from DESC, candidate.id DESC
                      LIMIT 1
                  )
                WHERE period.tenant_id = :tenantId
                  AND period.target_month = :targetMonth
                  AND period.deleted_at IS NULL
                """;
        return jdbcTemplate.query(sql, params, resultSet -> {
            if (!resultSet.next()) {
                return null;
            }
            int insuranceYear = resultSet.getInt("insurance_rate_year");
            return new CalculationBasis(
                    resultSet.getInt("income_tax_year"),
                    resultSet.getBoolean("child_care_support_required"),
                    resultSet.getBoolean("verified_flag"),
                    resultSet.getInt("tax_dependent_count"),
                    resultSet.getBoolean("income_tax_calc_flag"),
                    resultSet.getBoolean("resident_tax_calc_flag"),
                    resultSet.getBoolean("employment_insurance_flag"),
                    resultSet.getBoolean("social_insurance_flag"),
                    resultSet.getBoolean("health_insurance_flag"),
                    resultSet.getBoolean("pension_insurance_flag"),
                    resultSet.getBoolean("care_insurance_flag"),
                    resultSet.getBoolean("resident_tax_calc_flag")
                            ? resultSet.getBigDecimal("resident_tax")
                            : BigDecimal.ZERO,
                    resultSet.getBigDecimal("health_standard_remuneration"),
                    resultSet.getBigDecimal("pension_standard_remuneration"),
                    findRates(insuranceYear)
            );
        });
    }

    private Map<String, BigDecimal> findRates(int year) {
        Map<String, BigDecimal> rates = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT insurance_type, employee_rate
                        FROM insurance_rate_master
                        WHERE year = :year
                        ORDER BY id
                        """,
                Map.of("year", year),
                (resultSet, rowNumber) -> Map.entry(
                        resultSet.getString("insurance_type"),
                        resultSet.getBigDecimal("employee_rate")
                )).forEach(entry -> rates.putIfAbsent(
                        entry.getKey(), entry.getValue()
                ));
        return rates;
    }

    private BigDecimal findIncomeTax(
            int year,
            int dependents,
            BigDecimal taxable
    ) {
        return jdbcTemplate.query("""
                        SELECT tax_amount
                        FROM income_tax_table
                        WHERE year = :year
                          AND dependents = :dependents
                          AND :taxable BETWEEN min_salary AND max_salary
                        ORDER BY min_salary DESC, id DESC
                        LIMIT 1
                        """,
                Map.of(
                        "year", year,
                        "dependents", dependents,
                        "taxable", taxable
                ),
                resultSet -> resultSet.next()
                        ? resultSet.getBigDecimal("tax_amount")
                        : BigDecimal.ZERO);
    }

    private BigDecimal projectedMonthlyGross(EmployeeContract contract) {
        SalaryType salaryType = contract.getSalaryType();
        if (salaryType == null) {
            return BigDecimal.ZERO;
        }
        return switch (salaryType) {
            case MONTHLY -> nvl(contract.getMonthlySalary());
            case WEEKLY -> nvl(contract.getWeeklyWage())
                    .multiply(WEEKS_PER_YEAR)
                    .divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_UP);
            case DAILY -> nvl(contract.getDailyWage()).multiply(MONTHLY_WORK_DAYS);
            case HOURLY -> nvl(contract.getHourlyWage())
                    .multiply(nvl(contract.getStandardWorkingHours()))
                    .multiply(WEEKS_PER_YEAR)
                    .divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_UP);
        };
    }

    private BigDecimal rate(Map<String, BigDecimal> rates, String type) {
        return nvl(rates.get(type));
    }

    private BigDecimal yen(BigDecimal amount) {
        return nvl(amount).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record CalculationBasis(
            int incomeTaxYear,
            boolean childSupportRequired,
            boolean verified,
            int taxDependentCount,
            boolean incomeTax,
            boolean residentTaxEnabled,
            boolean employmentInsurance,
            boolean socialInsurance,
            boolean healthInsurance,
            boolean pensionInsurance,
            boolean careInsurance,
            BigDecimal residentTax,
            BigDecimal healthRemuneration,
            BigDecimal pensionRemuneration,
            Map<String, BigDecimal> rates
    ) { }
}
