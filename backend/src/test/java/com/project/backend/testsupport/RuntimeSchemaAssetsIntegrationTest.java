package com.project.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.MySQLContainer;

import com.project.backend.features.tax.dto.ResidentTaxConfirmRequest;
import com.project.backend.features.tax.dto.ResidentTaxDraftSaveRequest;
import com.project.backend.features.tax.dto.ResidentTaxEmployeeInput;
import com.project.backend.features.tax.dto.ResidentTaxMonthInput;
import com.project.backend.features.tax.service.ResidentTaxEditorService;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RuntimeSchemaAssetsIntegrationTest extends ContainerIntegrationTest {

    @Autowired
    private MySQLContainer<?> mysqlContainer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResidentTaxEditorService residentTaxEditorService;

    @Test
    void productionSchemaAssetsApplyToFreshMySql() throws Exception {
        List<String> resources = RuntimeSchemaAssetInstaller.readManifest();

        assertThat(resources).hasSize(32);
        RuntimeSchemaAssetInstaller.apply(mysqlContainer, resources);
        RuntimeSchemaAssetInstaller.apply(
                mysqlContainer,
                List.of("sql/system/import/tax_import_foundation_v1.sql")
        );

        assertThat(countTables(
                "monthly_closing_execution",
                "monthly_closing_output_definition",
                "monthly_closing_item",
                "payroll_item_balance_policy",
                "employee_payroll_item_enrollment",
                "employee_payroll_item_transaction",
                "annual_report_backup_setting",
                "annual_report_backup_execution",
                "annual_report_backup_file"
        )).isEqualTo(9);
        assertThat(countViews(
                "vw_daily_labor_cost_preview",
                "vw_daily_payment_preparation_preview",
                "vw_daily_pay_slip_latest",
                "vw_monthly_pay_slip_latest",
                "vw_employee_payroll_item_transaction_confirmed"
        )).isEqualTo(5);
        assertThat(countProcedures(
                "sp_daily_pay_slip_prepare",
                "sp_monthly_pay_slip_snapshot",
                "sp_monthly_invoice_snapshot",
                "sp_monthly_labor_cost_list_snapshot",
                "sp_daily_work_order_prepare"
        )).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM import_target WHERE target_code LIKE 'IMPORT_%TAX%'"
                        + " OR target_code IN ("
                        + "'IMPORT_HEALTH_INSURANCE_RATE',"
                        + "'IMPORT_CARE_INSURANCE_RATE',"
                        + "'IMPORT_PENSION_INSURANCE_RATE',"
                        + "'IMPORT_EMPLOYMENT_INSURANCE_RATE',"
                        + "'IMPORT_CHILD_CARE_SUPPORT_FUND')",
                Integer.class
        )).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM import_column column_def
                JOIN import_target target ON target.id = column_def.target_id
                WHERE target.target_code = 'IMPORT_CARE_INSURANCE_RATE'
                  AND column_def.deleted_at IS NULL
                """, Integer.class)).isEqualTo(4);

        assertCareInsuranceAndOfficialRatesReachMonthlyPayrollView();
    }

    private void assertCareInsuranceAndOfficialRatesReachMonthlyPayrollView() {
        jdbcTemplate.update("""
                INSERT INTO employee (
                    employee_code, employee_name, employment_type,
                    employment_status, active_flag, dormitory_flag,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    'TAX-001', '税計算確認者', 'FULL_TIME',
                    'ACTIVE', TRUE, FALSE,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, TEST_TENANT_ID);
        Long employeeId = jdbcTemplate.queryForObject(
                "SELECT id FROM employee WHERE employee_code = 'TAX-001'",
                Long.class
        );
        assertThat(employeeId).isNotNull();

        jdbcTemplate.update("""
                INSERT INTO employee_contract (
                    employee_id, renewal_flag, salary_type, payment_cycle,
                    monthly_salary, weekly_wage, daily_wage, hourly_wage,
                    standard_working_hours,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, FALSE, 'MONTHLY', 'MONTHLY',
                    300000, 0, 0, 0, 0,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeId, TEST_TENANT_ID);
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
                    TRUE, TRUE, 999,
                    TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 0,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeId, TEST_TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO company_profile (
                    company_code, company_name, active_flag,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    'INTEGRATION-COMPANY', '統合テスト会社', TRUE,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, TEST_TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO monthly_closings (
                    target_month, status, closing_version,
                    closing_start_date, closing_end_date,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    '2026-08-01', 'OPEN', 0,
                    '2026-08-01', '2026-08-31',
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, TEST_TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO employee_standard_remuneration (
                    employee_id, effective_from, effective_to,
                    health_standard_remuneration,
                    pension_standard_remuneration,
                    source_type, tenant_id, created_at, updated_at
                ) VALUES (
                    ?, '2026-04-01', NULL,
                    300000, 300000,
                    'REGULAR_DECISION', ?,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeId, TEST_TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO payroll_calculation_period (
                    target_month, income_tax_year, insurance_rate_year,
                    child_care_support_required, rounding_mode,
                    verified_flag, verified_at, verified_by, source_note,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    '2026-08-01', 2026, 2026,
                    TRUE, 'HALF_UP',
                    TRUE, CURRENT_TIMESTAMP(6), 'integration-test',
                    '2026年度公式料率の境界値確認',
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, TEST_TENANT_ID);
        jdbcTemplate.batchUpdate("""
                INSERT INTO insurance_rate_master (
                    insurance_type, year, employee_rate, employer_rate
                ) VALUES (?, 2026, ?, ?)
                """, List.of(
                new Object[]{"HEALTH_INSURANCE", "0.04805", "0.04805"},
                new Object[]{"CARE_INSURANCE", "0.00810", "0.00810"},
                new Object[]{"PENSION", "0.09150", "0.09150"},
                new Object[]{"EMPLOYMENT_INSURANCE", "0.00600", "0.01050"},
                new Object[]{"CHILD_CARE_SUPPORT", "0.00115", "0.00115"}
        ));
        jdbcTemplate.update("""
                INSERT INTO income_tax_table (
                    year, min_salary, max_salary, dependents, tax_amount
                ) VALUES (2026, 0, 999999999, 0, 10000)
                """);

        var residentTaxDraft = residentTaxEditorService.saveDraft(
                new ResidentTaxDraftSaveRequest(
                        2026,
                        List.of(new ResidentTaxEmployeeInput(
                                employeeId,
                                List.of(
                                        new ResidentTaxMonthInput(6, 12000),
                                        new ResidentTaxMonthInput(7, 12000),
                                        new ResidentTaxMonthInput(8, 12000),
                                        new ResidentTaxMonthInput(9, 12000),
                                        new ResidentTaxMonthInput(10, 12000),
                                        new ResidentTaxMonthInput(11, 12000),
                                        new ResidentTaxMonthInput(12, 12000),
                                        new ResidentTaxMonthInput(1, 12000),
                                        new ResidentTaxMonthInput(2, 12000),
                                        new ResidentTaxMonthInput(3, 12000),
                                        new ResidentTaxMonthInput(4, 12000),
                                        new ResidentTaxMonthInput(5, 12000)
                                )
                        ))
                )
        );
        residentTaxEditorService.confirm(
                residentTaxDraft.batchId(),
                new ResidentTaxConfirmRequest("締め統合テスト", false)
        );

        var calculation = jdbcTemplate.queryForMap("""
                SELECT
                    health_insurance,
                    child_care_contribution,
                    pension_insurance,
                    employment_insurance,
                    social_insurance_total,
                    taxable_amount,
                    income_tax,
                    resident_tax,
                    calculation_ready,
                    calculation_error_code
                FROM vw_monthly_pay_slip_tax_calculation
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                """, TEST_TENANT_ID, employeeId);

        assertAmount(calculation.get("health_insurance"), "16845");
        assertAmount(calculation.get("child_care_contribution"), "345");
        assertAmount(calculation.get("pension_insurance"), "27450");
        assertAmount(calculation.get("employment_insurance"), "1800");
        assertAmount(calculation.get("social_insurance_total"), "46440");
        assertAmount(calculation.get("taxable_amount"), "253560");
        assertAmount(calculation.get("income_tax"), "10000");
        assertAmount(calculation.get("resident_tax"), "12000");
        assertThat(((Number) calculation.get("calculation_ready")).intValue())
                .isEqualTo(1);
        assertThat(calculation.get("calculation_error_code")).isNull();

        Long mobileDeductionId = registerConfirmedAndDraftMobileTransactions(
                employeeId
        );
        assertThat(mobileDeductionId).isNotNull();

        String executionId = "RESIDENT-TAX-CLOSING-INTEGRATION";
        jdbcTemplate.update("""
                INSERT INTO monthly_pay_slip_input (
                    execution_id, target_month, employee_id,
                    closing_version, execution_mode,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, '2026-08', ?,
                    1, 'INITIAL',
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, executionId, employeeId, TEST_TENANT_ID);
        jdbcTemplate.execute("CALL sp_monthly_pay_slip_snapshot('" + executionId + "')");

        BigDecimal fixedResidentTax = jdbcTemplate.queryForObject("""
                SELECT resident_tax
                FROM monthly_pay_slip_history
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND closing_version = 1
                  AND employee_id = ?
                  AND deleted_at IS NULL
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(fixedResidentTax).isEqualByComparingTo("12000");

        BigDecimal fixedMobileDeduction = jdbcTemplate.queryForObject("""
                SELECT item.item_value
                FROM monthly_pay_slip_history_item item
                JOIN monthly_pay_slip_history history
                  ON history.id = item.monthly_pay_slip_history_id
                WHERE history.tenant_id = ?
                  AND history.target_month = '2026-08-01'
                  AND history.closing_version = 1
                  AND history.employee_id = ?
                  AND item.item_code = 'MOBILE_TEST'
                  AND item.deleted_at IS NULL
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(fixedMobileDeduction).isEqualByComparingTo("3500");
    }

    private Long registerConfirmedAndDraftMobileTransactions(Long employeeId) {
        jdbcTemplate.update("""
                INSERT INTO deduction_masters (
                    deduction_code, deduction_name, deduction_type,
                    calculation_type, default_amount, allow_manual_input,
                    deduction_unit, detail_view_type,
                    show_on_daily_statement, show_on_monthly_statement,
                    carry_to_monthly_settlement, display_order,
                    enabled, note, tenant_id, created_at, updated_at
                ) VALUES (
                    'MOBILE_TEST', '携帯料金テスト', 'COMPANY',
                    'FIXED', 0, TRUE,
                    'MONTHLY', 'NONE',
                    FALSE, TRUE,
                    TRUE, 500, TRUE,
                    'Testcontainers明細取引', ?,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, TEST_TENANT_ID);
        Long deductionId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM deduction_masters
                WHERE tenant_id = ?
                  AND deduction_code = 'MOBILE_TEST'
                  AND deleted_at IS NULL
                """, Long.class, TEST_TENANT_ID);

        jdbcTemplate.batchUpdate("""
                INSERT INTO employee_payroll_item_transaction (
                    employee_id, target_type, target_master_id,
                    target_code, target_name, target_month,
                    transaction_date, amount, quantity,
                    source_type, source_reference, status, note,
                    lock_version, tenant_id, created_at, updated_at
                ) VALUES (
                    ?, 'DEDUCTION', ?,
                    'MOBILE_TEST', '携帯料金テスト', '2026-08-01',
                    ?, ?, NULL,
                    'MANUAL', ?, ?, NULL,
                    0, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, List.of(
                new Object[]{
                        employeeId, deductionId, "2026-08-05", "2100",
                        "MOBILE-202608-1", "CONFIRMED", TEST_TENANT_ID
                },
                new Object[]{
                        employeeId, deductionId, "2026-08-12", "1400",
                        "MOBILE-202608-2", "CONFIRMED", TEST_TENANT_ID
                },
                new Object[]{
                        employeeId, deductionId, "2026-08-20", "9900",
                        "MOBILE-202608-DRAFT", "DRAFT", TEST_TENANT_ID
                }
        ));

        BigDecimal confirmedTotal = jdbcTemplate.queryForObject("""
                SELECT item_value
                FROM vw_monthly_pay_slip_variable_item
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                  AND item_code = 'MOBILE_TEST'
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(confirmedTotal).isEqualByComparingTo("3500");
        return deductionId;
    }

    private void assertAmount(Object actual, String expected) {
        assertThat(new BigDecimal(actual.toString()))
                .isEqualByComparingTo(expected);
    }

    private int countTables(String... names) {
        return countInformationSchemaObjects(
                "information_schema.tables",
                "table_name",
                names,
                ""
        );
    }

    private int countViews(String... names) {
        return countInformationSchemaObjects(
                "information_schema.views",
                "table_name",
                names,
                ""
        );
    }

    private int countProcedures(String... names) {
        String placeholders = String.join(",", java.util.Collections
                .nCopies(names.length, "?"));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.routines"
                        + " WHERE routine_schema = DATABASE()"
                        + " AND routine_type = 'PROCEDURE'"
                        + " AND routine_name IN (" + placeholders + ")",
                Integer.class,
                (Object[]) names
        );
        return count == null ? 0 : count;
    }

    private int countInformationSchemaObjects(
            String source,
            String nameColumn,
            String[] names,
            String extraCondition
    ) {
        String placeholders = String.join(",", java.util.Collections
                .nCopies(names.length, "?"));
        String sql = "SELECT COUNT(*) FROM " + source
                + " WHERE table_schema = DATABASE() AND " + nameColumn
                + " IN (" + placeholders + ")" + extraCondition;
        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                (Object[]) names
        );
        return count == null ? 0 : count;
    }
}
