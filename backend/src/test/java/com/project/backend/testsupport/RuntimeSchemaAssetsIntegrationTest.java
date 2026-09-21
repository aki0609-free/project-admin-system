package com.project.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.service.DailyPayComponentCalculationService;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.system.backup.dto.BackupExecutionResult;
import com.project.backend.features.system.backup.service.BackupExecutionService;
import com.project.backend.features.system.notice.dto.NoticeGenerateResult;
import com.project.backend.features.system.notice.service.NoticeAutoGenerateService;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RuntimeSchemaAssetsIntegrationTest extends ContainerIntegrationTest {

    @Autowired
    private MySQLContainer<?> mysqlContainer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResidentTaxEditorService residentTaxEditorService;

    @Autowired
    private BackupExecutionService backupExecutionService;

    @Autowired
    private NoticeAutoGenerateService noticeAutoGenerateService;

    @Autowired
    private DailyPayComponentCalculationService dailyPayComponentCalculationService;

    @Test
    void productionSchemaAssetsApplyToFreshMySql() throws Exception {
        List<String> resources = RuntimeSchemaAssetInstaller.readManifest();

        assertThat(resources)
                .hasSize(47)
                .contains(
                        "sql/admin/external_support_links_v1.sql",
                        "sql/application/applicant_legacy_schema_compatibility_v1.sql",
                        "sql/customer/customer_contract_status_v1.sql",
                        "sql/master/allowance_master_foundation_v1.sql",
                        "sql/daily_report/vehicle_arrangement_foundation_v1.sql",
                        "sql/operation/monthly/customer_transaction_adjustment_v1.sql"
                );
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
                "annual_report_backup_file",
                "customer_billing_closings",
                "monthly_order_form_input",
                "monthly_order_form_history",
                "monthly_order_form_render_execution",
                "employee_legal_deposit_refund"
        )).isEqualTo(14);
        assertThat(countViews(
                "vw_daily_labor_cost_preview",
                "vw_daily_payment_preparation_preview",
                "vw_daily_pay_slip_latest",
                "vw_monthly_pay_slip_latest",
                "vw_monthly_pay_slip_calculation_item_source",
                "vw_monthly_pay_slip_statement_item_source",
                "vw_monthly_pay_slip_deduction_basis",
                "vw_monthly_pay_slip_legal_deposit_refund",
                "vw_employee_payroll_item_transaction_confirmed",
                "vw_employee_legal_deposit_balance",
                "vw_monthly_order_form_render"
        )).isEqualTo(11);
        assertThat(countProcedures(
                "sp_daily_pay_slip_prepare",
                "sp_monthly_pay_slip_snapshot",
                "sp_monthly_invoice_snapshot",
                "sp_monthly_labor_cost_list_snapshot",
                "sp_daily_work_order_prepare",
                "sp_monthly_order_form_snapshot"
        )).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'daily_preparation_dispatches'
                  AND column_name = 'other_amount'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'monthly_invoice_history_detail'
                  AND column_name = 'other_amount'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'applicants'
                  AND column_name = 'dynamic_fields'
                  AND is_nullable = 'NO'
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM rule_master
                WHERE tenant_id = 'default'
                  AND rule_name IN (
                      'DAILY_NORMAL_PAY',
                      'DAILY_OVERTIME_PAY',
                      'DAILY_NIGHT_PAY',
                      'DAILY_HOLIDAY_PAY'
                  )
                  AND active_flag = TRUE
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM daily_pay_rule_setting
                WHERE tenant_id = 'default'
                  AND active_flag = TRUE
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM monthly_closing_output_definition
                WHERE tenant_id = 'default'
                  AND output_type = 'LEDGER'
                  AND output_code IN (
                      'MONTHLY_LABOR',
                      'LABOR_COST_PAYMENT',
                      'RECEIPT_CONFIRMATION',
                      'MONTHLY_SUMMARY'
                  )
                  AND required_flag = TRUE
                  AND active_flag = TRUE
                  AND backup_retention_years = 7
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(4);
        assertFoundationDailyPayRulesCalculateAmounts();
        assertDailyLaborCostSeparatesSalaryBasisAndPaymentCycle();
        assertDailyPaymentPreparationAggregatesEveryPaymentCycle();
        assertEmployeeCsvUsesCurrentEmployeeModel();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM allowance_masters
                WHERE allowance_code = 'ATTENDANCE_ATTITUDE'
                  AND calculation_type = 'MANUAL'
                  AND allowance_unit = 'BOTH'
                  AND rule_name IS NULL
                  AND allow_manual_input = TRUE
                  AND show_on_daily_statement = TRUE
                  AND show_on_monthly_statement = FALSE
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM allowance_masters
                WHERE tenant_id = 'default'
                  AND allowance_code = 'DRIVER_ALLOWANCE'
                  AND allowance_name = '運転手当'
                  AND calculation_type = 'AUTO'
                  AND allowance_unit = 'BOTH'
                  AND rule_name = 'DAILY_DRIVER_ALLOWANCE'
                  AND default_amount = 0
                  AND allow_manual_input = FALSE
                  AND min_amount = 0
                  AND show_on_daily_statement = TRUE
                  AND show_on_monthly_statement = TRUE
                  AND enabled = TRUE
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM allowance_masters
                WHERE tenant_id = 'default'
                  AND allowance_code = 'MANAGEMENT_ALLOWANCE'
                  AND allowance_name = '管理手当'
                  AND calculation_type = 'MANUAL'
                  AND allowance_unit = 'BOTH'
                  AND rule_name IS NULL
                  AND default_amount = 0
                  AND allow_manual_input = TRUE
                  AND min_amount = 0
                  AND show_on_daily_statement = TRUE
                  AND show_on_monthly_statement = TRUE
                  AND enabled = TRUE
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM rule_master rule
                JOIN rule_parameter parameter
                  ON parameter.rule_id = rule.id
                 AND parameter.deleted_at IS NULL
                WHERE rule.tenant_id = 'default'
                  AND rule.rule_name = 'DAILY_DRIVER_ALLOWANCE'
                  AND rule.dsl_text = 'vehicleArrangementType == ''EMPLOYEE'' ? mileage * distanceUnitPrice + passengerCount * passengerUnitPrice : 0'
                  AND rule.active_flag = TRUE
                  AND rule.deleted_at IS NULL
                  AND parameter.param_name IN (
                      'vehicleArrangementType',
                      'mileage',
                      'passengerCount',
                      'distanceUnitPrice',
                      'passengerUnitPrice'
                  )
                """, Integer.class)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM deduction_masters
                WHERE tenant_id = 'default'
                  AND deduction_code IN (
                      'INCOME_TAX',
                      'RESIDENT_TAX',
                      'HEALTH_INSURANCE',
                      'CHILD_SUPPORT',
                      'WELFARE_PENSION',
                      'EMPLOYMENT_INSURANCE',
                      'LEGAL_DEPOSIT',
                      'DORMITORY_FEE',
                      'MOBILE_RENTAL',
                      'WIFI_FEE'
                  )
                  AND calculation_type IN ('MANUAL', 'FIXED', 'AUTO')
                  AND detail_view_type IN (
                      'NONE',
                      'INCOME_TAX',
                      'RESIDENT_TAX',
                      'HEALTH_INSURANCE',
                      'CHILD_SUPPORT',
                      'PENSION',
                      'EMPLOYMENT_INSURANCE'
                  )
                  AND enabled = TRUE
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM deduction_masters deduction
                JOIN payroll_item_balance_policy policy
                  ON policy.target_master_id = deduction.id
                 AND policy.target_type = 'DEDUCTION'
                 AND policy.tenant_id = deduction.tenant_id
                 AND policy.deleted_at IS NULL
                WHERE deduction.tenant_id = 'default'
                  AND deduction.deduction_code = 'DORMITORY_FEE'
                  AND deduction.calculation_type = 'MANUAL'
                  AND deduction.rule_name IS NULL
                  AND policy.balance_unit = 'AMOUNT'
                  AND policy.balance_tracking_flag = TRUE
                  AND policy.accrual_rule_name =
                      'CALENDAR_DAYS_TIMES_PARAMETER:dormitoryDailyAmount'
                  AND policy.carry_forward_flag = TRUE
                  AND deduction.deleted_at IS NULL
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM deduction_masters
                WHERE tenant_id = 'default'
                  AND deduction_code = 'LEGAL_DEPOSIT'
                  AND calculation_type = 'AUTO'
                  AND rule_name = 'DAILY_LEGAL_DEPOSIT_ESTIMATE'
                  AND allow_manual_input = TRUE
                  AND deduction_unit = 'DAILY'
                  AND show_on_daily_statement = TRUE
                  AND show_on_monthly_statement = FALSE
                  AND carry_to_monthly_settlement = TRUE
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM rule_master rule
                JOIN rule_parameter parameter_item
                  ON parameter_item.rule_id = rule.id
                 AND parameter_item.param_name = 'predictedStatutoryDeductionTotal'
                 AND parameter_item.data_type = 'DECIMAL'
                 AND parameter_item.deleted_at IS NULL
                WHERE rule.tenant_id = 'default'
                  AND rule.rule_name = 'DAILY_LEGAL_DEPOSIT_ESTIMATE'
                  AND rule.rule_type = 'DEDUCTION'
                  AND rule.dsl_type = 'JEXL'
                  AND rule.dsl_text = 'predictedStatutoryDeductionTotal / 20'
                  AND rule.active_flag = TRUE
                  AND rule.deleted_at IS NULL
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM deduction_masters
                WHERE tenant_id = 'default'
                  AND (calculation_type IS NULL
                       OR calculation_type NOT IN ('MANUAL', 'FIXED', 'AUTO'))
                  AND deleted_at IS NULL
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM notice_rule
                WHERE tenant_id = 'default'
                  AND rule_code IN (
                      'CUSTOMER_CLOSING_DAY',
                      'COMPANY_PAYROLL_CLOSING_DAY'
                  )
                  AND target_date_source_type = 'DAY_RULE'
                  AND date_type = 'EXACT_DAY'
                  AND active_flag = TRUE
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM backup_target
                WHERE tenant_id = 'default'
                  AND target_code IN (
                      'BACKUP_CUSTOMERS',
                      'BACKUP_CUSTOMER_TRANSACTIONS',
                      'BACKUP_EMPLOYEES',
                      'BACKUP_DAILY_REPORTS'
                  )
                  AND output_mode = 'DOWNLOAD'
                  AND zip_required = TRUE
                  AND backup_enabled = TRUE
                  AND active_flag = TRUE
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM backup_target target
                WHERE target.tenant_id = 'default'
                  AND target.target_code IN (
                      'BACKUP_CUSTOMERS',
                      'BACKUP_CUSTOMER_TRANSACTIONS',
                      'BACKUP_EMPLOYEES',
                      'BACKUP_DAILY_REPORTS'
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM information_schema.columns source_column
                      WHERE source_column.table_schema = DATABASE()
                        AND source_column.table_name = target.table_name
                        AND NOT EXISTS (
                            SELECT 1
                            FROM backup_column definition_column
                            WHERE definition_column.target_id = target.id
                              AND definition_column.column_name = source_column.column_name
                              AND definition_column.export_flag = TRUE
                              AND definition_column.deleted_at IS NULL
                        )
                  )
                """, Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM deduction_masters
                WHERE tenant_id = 'default'
                  AND deduction_code = 'WIFI_FEE'
                  AND calculation_type = 'MANUAL'
                  AND default_amount = 1000
                  AND deduction_unit = 'DAILY'
                  AND show_on_daily_statement = TRUE
                  AND show_on_monthly_statement = FALSE
                  AND carry_to_monthly_settlement = FALSE
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM payroll_item_balance_policy policy
                JOIN deduction_masters deduction
                  ON deduction.id = policy.target_master_id
                 AND deduction.tenant_id = policy.tenant_id
                WHERE policy.tenant_id = 'default'
                  AND policy.target_type = 'DEDUCTION'
                  AND policy.target_code = 'WIFI_FEE'
                  AND policy.application_scope = 'EMPLOYEE_ENROLLMENT'
                  AND policy.input_source = 'DAILY_REPORT'
                  AND policy.balance_tracking_flag = FALSE
                  AND policy.carry_forward_flag = FALSE
                  AND policy.active_flag = TRUE
                  AND policy.deleted_at IS NULL
                  AND deduction.enabled = TRUE
                  AND deduction.deleted_at IS NULL
                """, Integer.class)).isEqualTo(1);
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
                FROM import_target
                WHERE target_code = 'IMPORT_EMPLOYMENT_INSURANCE_RATE'
                  AND active_flag = TRUE
                  AND script_args LIKE '%--category CONSTRUCTION%'
                  AND deleted_at IS NULL
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM import_column column_def
                JOIN import_target target ON target.id = column_def.target_id
                WHERE target.target_code = 'IMPORT_CARE_INSURANCE_RATE'
                  AND column_def.deleted_at IS NULL
                """, Integer.class)).isEqualTo(4);

        assertClosingDayNoticesAreGeneratedOnlyOnce();
        assertBusinessDataBackupDownloadsAsZip();
        assertCareInsuranceAndOfficialRatesReachMonthlyPayrollView();
    }

    private void assertFoundationDailyPayRulesCalculateAmounts() {
        TenantContext.setTenantId("default");
        try {
            DailyReport report = new DailyReport();
            report.setWorkDate(LocalDate.of(2026, 8, 3));
            report.setWorkHours(new BigDecimal("8"));
            report.setOvertimeHours(new BigDecimal("2"));
            report.setNightWorkHours(BigDecimal.ONE);
            report.setHolidayWorkHours(BigDecimal.ZERO);

            EmployeeContract contract = new EmployeeContract();
            contract.setSalaryType(SalaryType.HOURLY);
            contract.setHourlyWage(new BigDecimal("1000"));

            var amounts = dailyPayComponentCalculationService.calculate(
                    report,
                    contract,
                    null
            );

            assertThat(amounts.normalPayAmount())
                    .isEqualByComparingTo("8000");
            assertThat(amounts.overtimePayAmount())
                    .isEqualByComparingTo("2500");
            assertThat(amounts.nightPayAmount())
                    .isEqualByComparingTo("250");
            assertThat(amounts.holidayPayAmount()).isZero();
            assertThat(amounts.total()).isEqualByComparingTo("10750");
        } finally {
            TenantContext.clear();
        }
    }

    private void assertClosingDayNoticesAreGeneratedOnlyOnce() {
        TenantContext.setTenantId("default");
        try {
            jdbcTemplate.update("""
                    INSERT INTO customers (
                        name, invoice_type,
                        closing_day_type, closing_day_value,
                        closing_month_offset, payment_month_offset,
                        tenant_id, created_at, updated_at
                    ) VALUES (
                        '締日通知テスト顧客', 'PATTERN_1',
                        'DAY_OF_MONTH', 1,
                        0, 0,
                        'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                    )
                    """);
            jdbcTemplate.update("""
                    DELETE FROM closing_setting
                    WHERE tenant_id = 'default'
                      AND setting_code = 'PAYROLL'
                    """);
            jdbcTemplate.update("""
                    INSERT INTO closing_setting (
                        setting_code,
                        closing_day_type, closing_day_value,
                        closing_month_offset, payment_month_offset,
                        active_flag,
                        tenant_id, created_at, updated_at
                    ) VALUES (
                        'PAYROLL',
                        'DAY_OF_MONTH', 1,
                        0, 0,
                        TRUE,
                        'default', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                    )
                    """);

            NoticeGenerateResult first = noticeAutoGenerateService.generateAll();
            NoticeGenerateResult second = noticeAutoGenerateService.generateAll();

            assertThat(first.ruleCount()).isEqualTo(2);
            assertThat(first.generatedCount()).isEqualTo(2);
            assertThat(second.generatedCount()).isZero();
            assertThat(second.skippedCount()).isEqualTo(2);
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM notices
                    WHERE tenant_id = 'default'
                      AND source_rule_code IN (
                          'CUSTOMER_CLOSING_DAY',
                          'COMPANY_PAYROLL_CLOSING_DAY'
                      )
                      AND deleted_at IS NULL
                    """, Integer.class)).isEqualTo(2);
        } finally {
            TenantContext.setTenantId(TEST_TENANT_ID);
        }
    }

    private void assertBusinessDataBackupDownloadsAsZip() throws Exception {
        TenantContext.setTenantId("default");
        try {
            BackupExecutionResult result = backupExecutionService.execute(List.of(
                    "BACKUP_CUSTOMERS",
                    "BACKUP_CUSTOMER_TRANSACTIONS",
                    "BACKUP_EMPLOYEES",
                    "BACKUP_DAILY_REPORTS"
            ));

            assertThat(result.zipOutput()).isTrue();
            assertThat(result.contentType()).isEqualTo("application/zip");
            assertThat(result.fileName()).endsWith(".zip");
            assertThat(result.storedFile()).isNull();

            Set<String> entryNames = new LinkedHashSet<>();
            try (ZipInputStream zip = new ZipInputStream(
                    new ByteArrayInputStream(result.data())
            )) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    entryNames.add(entry.getName());
                    zip.closeEntry();
                }
            }

            assertThat(entryNames).hasSize(4);
            assertThat(entryNames).anyMatch(name -> name.startsWith("customers_"));
            assertThat(entryNames).anyMatch(name -> name.startsWith("customer_transactions_"));
            assertThat(entryNames).anyMatch(name -> name.startsWith("employees_"));
            assertThat(entryNames).anyMatch(name -> name.startsWith("daily_reports_"));
        } finally {
            TenantContext.setTenantId(TEST_TENANT_ID);
        }
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
                UPDATE deduction_masters
                SET tenant_id = ?, updated_at = CURRENT_TIMESTAMP(6)
                WHERE tenant_id = 'default'
                  AND deduction_code = 'LEGAL_DEPOSIT'
                  AND deleted_at IS NULL
                """, TEST_TENANT_ID);
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
                INSERT INTO daily_report (
                    employee_id, work_date, normal_pay_amount,
                    billing_base_unit_price, billing_overtime_unit_price,
                    billing_night_unit_price, billing_holiday_unit_price,
                    billing_commute_unit_price, holiday_premium_eligible,
                    dormitory_charge_days, overtime_pay_amount,
                    night_pay_amount, holiday_pay_amount,
                    estimated_gross_pay_amount, estimated_net_pay_amount,
                    vehicle_used_flag,
                    approval_status, tenant_id, created_at, updated_at
                ) VALUES (
                    ?, '2026-08-03', 300000,
                    0, 0, 0, 0, 0, FALSE,
                    0, 0, 0, 0, 300000, 300000, FALSE,
                    'APPROVED', ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeId, TEST_TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO daily_report_deductions (
                    daily_report_id, deduction_master_id,
                    deduction_code, deduction_name,
                    amount, calculated_amount, manual_override_flag,
                    quantity, balance_unit,
                    tenant_id, created_at, updated_at
                )
                SELECT report.id, master.id,
                       master.deduction_code, master.deduction_name,
                       70000, 70000, FALSE,
                       70000, 'AMOUNT', report.tenant_id,
                       CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                FROM daily_report report
                JOIN deduction_masters master
                  ON master.tenant_id = report.tenant_id
                 AND master.deduction_code = 'LEGAL_DEPOSIT'
                 AND master.deleted_at IS NULL
                WHERE report.tenant_id = ?
                  AND report.employee_id = ?
                  AND report.work_date = '2026-08-03'
                  AND report.deleted_at IS NULL
                """, TEST_TENANT_ID, employeeId);
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

        BigDecimal residentTaxWithoutConfirmedMonthlyValue = jdbcTemplate.queryForObject("""
                SELECT resident_tax
                FROM vw_monthly_pay_slip_employee_month
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(residentTaxWithoutConfirmedMonthlyValue)
                .as("従業員給与設定の旧住民税月額はフォールバックに使用しない")
                .isEqualByComparingTo("0");

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

        jdbcTemplate.update("""
                INSERT INTO daily_report_allowances (
                    daily_report_id, allowance_master_id,
                    allowance_code, allowance_name,
                    amount, calculated_amount, manual_override_flag,
                    quantity, balance_unit,
                    tenant_id, created_at, updated_at
                )
                SELECT report.id, master.id,
                       configured.allowance_code, master.allowance_name,
                       configured.amount, configured.amount, FALSE,
                       configured.amount, 'AMOUNT', report.tenant_id,
                       CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                FROM daily_report report
                CROSS JOIN (
                    SELECT 'ATTENDANCE_ATTITUDE' AS allowance_code, 1000 AS amount
                    UNION ALL SELECT 'DRIVER_ALLOWANCE', 2500
                    UNION ALL SELECT 'MANAGEMENT_ALLOWANCE', 3000
                ) configured
                JOIN allowance_masters master
                  ON master.tenant_id = 'default'
                 AND master.allowance_code = configured.allowance_code
                 AND master.deleted_at IS NULL
                WHERE report.tenant_id = ?
                  AND report.employee_id = ?
                  AND report.work_date = '2026-08-03'
                  AND report.deleted_at IS NULL
                """, TEST_TENANT_ID, employeeId);

        Long mobileDeductionId = registerConfirmedAndDraftMobileTransactions(
                employeeId
        );
        assertThat(mobileDeductionId).isNotNull();
        Long temporaryAllowanceId = registerConfirmedAndDraftAllowanceTransactions(
                employeeId
        );
        assertThat(temporaryAllowanceId).isNotNull();

        Map<String, Object> refundStatement = jdbcTemplate.queryForMap("""
                SELECT latest.legal_deposit_refund_amount,
                       latest.gross_amount,
                       settlement.legal_deposit_amount,
                       settlement.legal_deduction_total
                FROM vw_monthly_pay_slip_latest latest
                JOIN vw_monthly_pay_slip_legal_deposit_refund settlement
                  ON settlement.tenant_id = latest.tenant_id
                 AND settlement.target_month = latest.target_month
                 AND settlement.employee_id = latest.employee_id
                WHERE latest.tenant_id = ?
                  AND latest.target_month = '2026-08-01'
                  AND latest.employee_id = ?
                """, TEST_TENANT_ID, employeeId);
        assertAmount(refundStatement.get("legal_deposit_amount"), "70000");
        BigDecimal expectedLegalDepositRefund = new BigDecimal("70000")
                .subtract(new BigDecimal(
                        refundStatement.get("legal_deduction_total").toString()
                ));
        assertThat(new BigDecimal(
                refundStatement.get("legal_deposit_refund_amount").toString()
        )).isEqualByComparingTo(expectedLegalDepositRefund);

        List<Map<String, Object>> dailyAllowanceTotals = jdbcTemplate.queryForList("""
                SELECT item_code, item_value
                FROM vw_monthly_pay_slip_variable_item
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                  AND item_code IN (
                      'ATTENDANCE_ATTITUDE',
                      'DRIVER_ALLOWANCE',
                      'MANAGEMENT_ALLOWANCE'
                  )
                ORDER BY item_code
                """, TEST_TENANT_ID, employeeId);
        assertThat(dailyAllowanceTotals).hasSize(3);
        assertThat(dailyAllowanceTotals).anySatisfy(row -> {
            assertThat(row.get("item_code")).isEqualTo("ATTENDANCE_ATTITUDE");
            assertAmount(row.get("item_value"), "1000");
        }).anySatisfy(row -> {
            assertThat(row.get("item_code")).isEqualTo("DRIVER_ALLOWANCE");
            assertAmount(row.get("item_value"), "2500");
        }).anySatisfy(row -> {
            assertThat(row.get("item_code")).isEqualTo("MANAGEMENT_ALLOWANCE");
            assertAmount(row.get("item_value"), "3000");
        });

        Integer legalDepositDeductionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM vw_monthly_pay_slip_variable_item
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                  AND item_category = 'LEGAL_DEDUCTION'
                  AND item_code = 'LEGAL_DEPOSIT'
                """, Integer.class, TEST_TENANT_ID, employeeId);
        assertThat(legalDepositDeductionCount).isZero();

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

        BigDecimal fixedTemporaryAllowance = jdbcTemplate.queryForObject("""
                SELECT item.item_value
                FROM monthly_pay_slip_history_item item
                JOIN monthly_pay_slip_history history
                  ON history.id = item.monthly_pay_slip_history_id
                WHERE history.tenant_id = ?
                  AND history.target_month = '2026-08-01'
                  AND history.closing_version = 1
                  AND history.employee_id = ?
                  AND item.item_code = 'TEMPORARY_ALLOWANCE_TEST'
                  AND item.deleted_at IS NULL
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(fixedTemporaryAllowance).isEqualByComparingTo("4200");

        BigDecimal fixedLegalDepositRefund = jdbcTemplate.queryForObject("""
                SELECT history.legal_deposit_refund_amount
                FROM monthly_pay_slip_history history
                WHERE history.tenant_id = ?
                  AND history.target_month = '2026-08-01'
                  AND history.closing_version = 1
                  AND history.employee_id = ?
                  AND history.deleted_at IS NULL
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(fixedLegalDepositRefund)
                .isEqualByComparingTo(expectedLegalDepositRefund);

        assertRetryUsesHistoryAndRecloseCreatesNewVersion(
                employeeId,
                executionId
        );
    }

    private void assertRetryUsesHistoryAndRecloseCreatesNewVersion(
            Long employeeId,
            String initialExecutionId
    ) {
        jdbcTemplate.update("""
                UPDATE resident_tax_monthly
                SET tax_amount = 13000,
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE tenant_id = ?
                  AND employee_id = ?
                  AND fiscal_year = 2026
                  AND month = 8
                  AND deleted_at IS NULL
                """, TEST_TENANT_ID, employeeId);

        BigDecimal latestViewAmount = jdbcTemplate.queryForObject("""
                SELECT resident_tax
                FROM vw_monthly_pay_slip_latest
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(latestViewAmount).isEqualByComparingTo("13000");

        jdbcTemplate.update("""
                UPDATE monthly_pay_slip_input
                SET execution_mode = 'RETRY',
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE execution_id = ?
                  AND tenant_id = ?
                  AND deleted_at IS NULL
                """, initialExecutionId, TEST_TENANT_ID);
        jdbcTemplate.execute(
                "CALL sp_monthly_pay_slip_snapshot('"
                        + initialExecutionId
                        + "')"
        );

        BigDecimal retryOutputAmount = jdbcTemplate.queryForObject("""
                SELECT resident_tax
                FROM monthly_pay_slip_render_output
                WHERE tenant_id = ?
                  AND execution_id = ?
                  AND employee_id = ?
                  AND deleted_at IS NULL
                """, BigDecimal.class,
                TEST_TENANT_ID, initialExecutionId, employeeId);
        assertThat(retryOutputAmount).isEqualByComparingTo("12000");

        String recloseExecutionId = "RESIDENT-TAX-RECLOSE-INTEGRATION";
        jdbcTemplate.update("""
                INSERT INTO monthly_pay_slip_input (
                    execution_id, target_month, employee_id,
                    closing_version, execution_mode,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, '2026-08', ?,
                    2, 'RECLOSE',
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, recloseExecutionId, employeeId, TEST_TENANT_ID);
        jdbcTemplate.execute(
                "CALL sp_monthly_pay_slip_snapshot('"
                        + recloseExecutionId
                        + "')"
        );

        List<BigDecimal> historyAmounts = jdbcTemplate.queryForList("""
                SELECT resident_tax
                FROM monthly_pay_slip_history
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                  AND deleted_at IS NULL
                ORDER BY closing_version
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(historyAmounts).hasSize(2);
        assertAmount(historyAmounts.get(0), "12000");
        assertAmount(historyAmounts.get(1), "13000");
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
                    FALSE, FALSE,
                    FALSE, 500, TRUE,
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

        jdbcTemplate.update("""
                INSERT INTO employee_payroll_item_transaction (
                    employee_id, target_type, target_master_id,
                    target_code, target_name, target_month,
                    transaction_date, amount, quantity,
                    transaction_purpose, balance_effect,
                    source_type, source_reference, status, note,
                    lock_version, tenant_id, created_at, updated_at
                ) VALUES (
                    ?, 'DEDUCTION', ?,
                    'MOBILE_TEST', '携帯料金テスト', '2026-08-01',
                    '2026-08-03', 10000, 10000,
                    'BALANCE_ACCRUAL', 'CREDIT',
                    'MANUAL', 'MOBILE-202608-BILL', 'CONFIRMED',
                    '請求額は残高だけへ反映し、月次控除へ直接含めない',
                    0, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeId, deductionId, TEST_TENANT_ID);

        BigDecimal confirmedTotal = jdbcTemplate.queryForObject("""
                SELECT item_value
                FROM vw_monthly_pay_slip_variable_item
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                  AND item_code = 'MOBILE_TEST'
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(confirmedTotal).isEqualByComparingTo("3500");

        BigDecimal calculationTotal = jdbcTemplate.queryForObject("""
                SELECT item_value
                FROM vw_monthly_pay_slip_calculation_item_source
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                  AND item_code = 'MOBILE_TEST'
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        BigDecimal statementTotal = jdbcTemplate.queryForObject("""
                SELECT item_value
                FROM vw_monthly_pay_slip_statement_item_source
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                  AND item_code = 'MOBILE_TEST'
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(calculationTotal).isEqualByComparingTo("3500");
        assertThat(statementTotal).isEqualByComparingTo("3500");
        return deductionId;
    }

    private Long registerConfirmedAndDraftAllowanceTransactions(Long employeeId) {
        jdbcTemplate.update("""
                INSERT INTO allowance_masters (
                    allowance_code, allowance_name, allowance_type,
                    calculation_type, allowance_unit, detail_view_type,
                    taxable, show_on_daily_statement,
                    show_on_monthly_statement, display_order,
                    enabled, note, tenant_id, created_at, updated_at
                ) VALUES (
                    'TEMPORARY_ALLOWANCE_TEST', '臨時手当テスト', 'COMPANY',
                    'MANUAL', 'MONTHLY', 'NONE',
                    TRUE, FALSE,
                    FALSE, 510,
                    TRUE, 'Testcontainers手当明細取引', ?,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, TEST_TENANT_ID);
        Long allowanceId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM allowance_masters
                WHERE tenant_id = ?
                  AND allowance_code = 'TEMPORARY_ALLOWANCE_TEST'
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
                    ?, 'ALLOWANCE', ?,
                    'TEMPORARY_ALLOWANCE_TEST', '臨時手当テスト', '2026-08-01',
                    ?, ?, NULL,
                    'MANUAL', ?, ?, NULL,
                    0, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, List.of(
                new Object[]{
                        employeeId, allowanceId, "2026-08-10", "4200",
                        "ALLOWANCE-202608-1", "CONFIRMED", TEST_TENANT_ID
                },
                new Object[]{
                        employeeId, allowanceId, "2026-08-15", "8800",
                        "ALLOWANCE-202608-DRAFT", "DRAFT", TEST_TENANT_ID
                }
        ));

        BigDecimal calculationTotal = jdbcTemplate.queryForObject("""
                SELECT item_value
                FROM vw_monthly_pay_slip_calculation_item_source
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                  AND item_category = 'ALLOWANCE'
                  AND item_code = 'TEMPORARY_ALLOWANCE_TEST'
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        BigDecimal statementTotal = jdbcTemplate.queryForObject("""
                SELECT item_value
                FROM vw_monthly_pay_slip_statement_item_source
                WHERE tenant_id = ?
                  AND target_month = '2026-08-01'
                  AND employee_id = ?
                  AND item_category = 'ALLOWANCE'
                  AND item_code = 'TEMPORARY_ALLOWANCE_TEST'
                """, BigDecimal.class, TEST_TENANT_ID, employeeId);
        assertThat(calculationTotal).isEqualByComparingTo("4200");
        assertThat(statementTotal).isEqualByComparingTo("4200");
        return allowanceId;
    }

    private void assertAmount(Object actual, String expected) {
        assertThat(new BigDecimal(actual.toString()))
                .isEqualByComparingTo(expected);
    }

    private void assertDailyPaymentPreparationAggregatesEveryPaymentCycle() {
        LocalDate paymentDate = LocalDate.of(2026, 9, 10);
        Long dailyEmployeeId = insertPaymentPreparationEmployee(
                "PAY-PREP-D", "支払日次", "DAILY"
        );
        Long weeklyEmployeeId = insertPaymentPreparationEmployee(
                "PAY-PREP-W", "支払週次", "WEEKLY"
        );
        Long monthlyEmployeeId = insertPaymentPreparationEmployee(
                "PAY-PREP-M", "支払月次", "MONTHLY"
        );

        insertPaymentPreparationReport(
                dailyEmployeeId, LocalDate.of(2026, 9, 10), paymentDate,
                "6000", "0", "0", "6000"
        );
        insertPaymentPreparationReport(
                weeklyEmployeeId, LocalDate.of(2026, 9, 4), paymentDate,
                "3000", "100", "0", "3000"
        );
        insertPaymentPreparationReport(
                weeklyEmployeeId, LocalDate.of(2026, 9, 9), paymentDate,
                "3000", "200", "0", "3000"
        );
        insertPaymentPreparationReport(
                monthlyEmployeeId, LocalDate.of(2026, 9, 8), paymentDate,
                "13063", "500", "500", "12563"
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT *
                FROM vw_daily_payment_preparation_preview
                WHERE tenant_id = ?
                  AND target_date = ?
                ORDER BY payment_cycle_order, employee_code
                """, TEST_TENANT_ID, paymentDate);

        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(row -> row.get("payment_cycle"))
                .containsExactly("DAILY", "WEEKLY", "MONTHLY");
        assertAmount(rows.get(1).get("gross_payment_amount"), "6000");
        assertAmount(rows.get(1).get("allowance_amount"), "300");
        assertAmount(rows.getFirst().get("total_net_payment_amount"), "24563");

        Map<String, Object> summary = rows.getFirst();
        assertAmount(summary.get("bill_10000"), "1");
        assertAmount(summary.get("bill_5000"), "2");
        assertAmount(summary.get("bill_1000"), "4");
        assertAmount(summary.get("coin_500"), "1");
        assertAmount(summary.get("coin_100"), "0");
        assertAmount(summary.get("coin_50"), "1");
        assertAmount(summary.get("coin_10"), "1");
        assertAmount(summary.get("coin_5"), "0");
        assertAmount(summary.get("coin_1"), "3");
    }

    private void assertEmployeeCsvUsesCurrentEmployeeModel() {
        Long employeeId = insertLaborCostEmployee(
                "CSV-CURRENT-001", "CSV現行形式", "WEEKLY", "WEEKLY",
                "0", "75000", "0"
        );
        jdbcTemplate.update("""
                INSERT INTO employee_payroll_profile (
                    employee_id, tax_category, tax_dependent_count,
                    dependent_flag, dependent_of_other_flag,
                    paid_leave_remaining_days,
                    income_tax_calc_flag, resident_tax_calc_flag,
                    resident_tax_monthly, employment_insurance_flag,
                    social_insurance_flag, health_insurance_flag,
                    pension_insurance_flag, care_insurance_flag,
                    daily_pay_flag, commute_allowance_monthly,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, 'KOU', 2, FALSE, FALSE, 12.5,
                    TRUE, TRUE, 8500, TRUE,
                    TRUE, TRUE, TRUE, FALSE,
                    FALSE, 12000,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeId, TEST_TENANT_ID);

        String executionId = "employee-csv-current-model";
        jdbcTemplate.update("""
                INSERT INTO employee_csv_input (
                    execution_id, include_deleted, tenant_id,
                    created_at, updated_at
                ) VALUES (?, FALSE, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, executionId, TEST_TENANT_ID);
        jdbcTemplate.update("CALL sp_employee_csv_prepare(?)", executionId);

        Map<String, Object> output = jdbcTemplate.queryForMap("""
                SELECT tax_category, tax_dependent_count,
                       paid_leave_remaining_days,
                       contract_start_date, salary_type, payment_cycle,
                       weekly_wage, standard_working_hours
                FROM employee_csv_output
                WHERE execution_id = ? AND employee_code = 'CSV-CURRENT-001'
                """, executionId);
        assertThat(output.get("tax_category")).isEqualTo("KOU");
        assertThat(output.get("tax_dependent_count")).isEqualTo(2);
        assertAmount(output.get("paid_leave_remaining_days"), "12.5");
        assertThat(output.get("salary_type")).isEqualTo("WEEKLY");
        assertThat(output.get("payment_cycle")).isEqualTo("WEEKLY");
        assertAmount(output.get("weekly_wage"), "75000");
        assertAmount(output.get("standard_working_hours"), "8");

        String querySql = jdbcTemplate.queryForObject("""
                SELECT query_sql
                FROM report_master
                WHERE tenant_id = 'default' AND report_code = 'EMPLOYEE_CSV'
                """, String.class);
        assertThat(querySql)
                .contains("給与計算基準")
                .contains("従業員別手当・控除設定")
                .doesNotContain("住民税月額")
                .doesNotContain("入寮区分")
                .doesNotContain("寮タイプ");

        jdbcTemplate.update("CALL sp_employee_csv_cleanup(?)", executionId);
    }

    private void assertDailyLaborCostSeparatesSalaryBasisAndPaymentCycle() {
        LocalDate workDate = LocalDate.of(2026, 9, 11);
        LocalDate monthlyPaymentDate = LocalDate.of(2026, 9, 30);

        Long dailyPaidDaily = insertLaborCostEmployee(
                "LABOR-DAILY-DAILY", "日給・日払い", "DAILY", "DAILY",
                "12000", "0", "0"
        );
        jdbcTemplate.update("""
                UPDATE employee
                SET postal_code = '123-4567', address = '東京都千代田区テスト1-2-3'
                WHERE id = ? AND tenant_id = ?
                """, dailyPaidDaily, TEST_TENANT_ID);
        Long dailyPaidMonthly = insertLaborCostEmployee(
                "LABOR-DAILY-MONTHLY", "日給・月払い", "DAILY", "MONTHLY",
                "14500", "0", "0"
        );
        Long monthlyPaidMonthly = insertLaborCostEmployee(
                "LABOR-MONTHLY-MONTHLY", "月給・月払い", "MONTHLY", "MONTHLY",
                "0", "0", "300000"
        );
        Long weeklyPaidWeekly = insertLaborCostEmployee(
                "LABOR-WEEKLY-WEEKLY", "週給・週払い", "WEEKLY", "WEEKLY",
                "0", "75000", "0"
        );

        insertPaymentPreparationReport(
                dailyPaidDaily, workDate, workDate,
                "12000", "0", "1000", "11000"
        );
        insertPaymentPreparationReport(
                dailyPaidMonthly, workDate, monthlyPaymentDate,
                "14500", "0", "1000", "13500"
        );
        insertPaymentPreparationReport(
                monthlyPaidMonthly, workDate, monthlyPaymentDate,
                "15000", "0", "3000", "12000"
        );
        insertPaymentPreparationReport(
                weeklyPaidWeekly, workDate, workDate.plusDays(1),
                "15000", "0", "3000", "12000"
        );

        List<Map<String, Object>> workDateRows = jdbcTemplate.queryForList("""
                SELECT *
                FROM vw_daily_labor_cost_preview
                WHERE tenant_id = ?
                  AND target_date = ?
                ORDER BY employee_code
                """, TEST_TENANT_ID, workDate);

        assertThat(workDateRows).hasSize(4);
        Map<String, Map<String, Object>> rowByCode = workDateRows.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> row.get("employee_code").toString(),
                        row -> row
                ));
        assertAmount(rowByCode.get("LABOR-DAILY-DAILY").get("gross_payment_amount"), "12000");
        assertAmount(rowByCode.get("LABOR-DAILY-DAILY").get("payment_amount"), "12000");
        assertAmount(rowByCode.get("LABOR-DAILY-MONTHLY").get("gross_payment_amount"), "14500");
        assertAmount(rowByCode.get("LABOR-DAILY-MONTHLY").get("payment_amount"), "0");
        assertAmount(rowByCode.get("LABOR-MONTHLY-MONTHLY").get("gross_payment_amount"), "15000");
        assertAmount(rowByCode.get("LABOR-MONTHLY-MONTHLY").get("payment_amount"), "0");
        assertAmount(rowByCode.get("LABOR-WEEKLY-WEEKLY").get("gross_payment_amount"), "15000");
        assertAmount(rowByCode.get("LABOR-WEEKLY-WEEKLY").get("payment_amount"), "0");

        Long dynamicAllowanceId = insertDailyStatementMaster(
                "ALLOWANCE", "DYNAMIC_DAILY_ALLOWANCE", "動的日次手当", 1
        );
        Long dynamicDeductionId = insertDailyStatementMaster(
                "DEDUCTION", "DYNAMIC_DAILY_DEDUCTION", "動的日次控除", 1
        );
        insertAllEmployeeDailyPolicy(
                "ALLOWANCE", dynamicAllowanceId,
                "DYNAMIC_DAILY_ALLOWANCE", "動的日次手当"
        );
        insertAllEmployeeDailyPolicy(
                "DEDUCTION", dynamicDeductionId,
                "DYNAMIC_DAILY_DEDUCTION", "動的日次控除"
        );
        jdbcTemplate.update("""
                UPDATE payroll_item_balance_policy
                SET balance_tracking_flag = TRUE,
                    accrual_rule_name = 'MANUAL_TRANSACTION',
                    carry_forward_flag = TRUE
                WHERE tenant_id = ? AND target_type = 'DEDUCTION'
                  AND target_code = 'DYNAMIC_DAILY_DEDUCTION'
                """, TEST_TENANT_ID);
        jdbcTemplate.update("""
                INSERT INTO employee_payroll_item_transaction (
                    employee_id, target_type, target_master_id,
                    target_code, target_name, target_month, transaction_date,
                    amount, quantity, transaction_purpose, balance_effect,
                    source_type, source_reference, status, lock_version,
                    tenant_id, created_at, updated_at
                ) VALUES (?, 'DEDUCTION', ?, 'DYNAMIC_DAILY_DEDUCTION',
                          '動的日次控除', ?, ?, 5000, 5000,
                          'BALANCE_ACCRUAL', 'CREDIT', 'MANUAL',
                          'daily-slip-balance-test', 'CONFIRMED', 0, ?,
                          CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, dailyPaidDaily, dynamicDeductionId,
                workDate.withDayOfMonth(1), workDate, TEST_TENANT_ID);

        List<Map<String, Object>> dynamicItems = jdbcTemplate.queryForList("""
                SELECT item_code, item_value
                FROM vw_daily_pay_slip_item_source
                WHERE tenant_id = ?
                  AND payment_date = ?
                  AND employee_id = ?
                  AND item_code IN ('DYNAMIC_DAILY_ALLOWANCE', 'DYNAMIC_DAILY_DEDUCTION')
                ORDER BY item_code
                """, TEST_TENANT_ID, workDate, dailyPaidDaily);
        assertThat(dynamicItems).hasSize(2);
        dynamicItems.forEach(row -> assertAmount(row.get("item_value"), "0"));

        Map<String, Object> dailySlip = jdbcTemplate.queryForMap("""
                SELECT payment_date_label, labor_period_from_label, employee_address,
                       work_hours_label, overtime_hours_label,
                       allowance_item_name1, allowance_item_value1,
                       deduction_item_name1, deduction_item_value1,
                       deduction_total, note
                FROM vw_daily_pay_slip_latest
                WHERE tenant_id = ?
                  AND payment_date = ?
                  AND employee_id = ?
                """, TEST_TENANT_ID, workDate, dailyPaidDaily);
        assertThat(dailySlip.get("payment_date_label")).isEqualTo("2026年9月11日");
        assertThat(dailySlip.get("labor_period_from_label")).isEqualTo("2026年9月11日");
        assertThat(dailySlip.get("employee_address"))
                .isEqualTo("〒123-4567 東京都千代田区テスト1-2-3");
        assertThat(dailySlip.get("work_hours_label")).isEqualTo("8時間");
        assertThat(dailySlip.get("overtime_hours_label")).isEqualTo("0分");
        assertThat(dailySlip.get("allowance_item_name1")).isEqualTo("動的日次手当");
        assertAmount(dailySlip.get("allowance_item_value1"), "0");
        assertThat(dailySlip.get("deduction_item_name1"))
                .isEqualTo("動的日次控除（残高：5,000円）");
        assertAmount(dailySlip.get("deduction_item_value1"), "0");
        assertAmount(dailySlip.get("deduction_total"), "1000");
        assertThat(dailySlip.get("note")).isEqualTo("Testcontainers日次給与明細備考");

        String slipExecutionId = "daily-slip-review-001";
        jdbcTemplate.update("""
                INSERT INTO daily_pay_slip_input (
                    tenant_id, created_at, updated_at,
                    execution_id, payment_date, employee_id
                ) VALUES (?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), ?, ?, ?)
                """, TEST_TENANT_ID, slipExecutionId, workDate, dailyPaidDaily);
        jdbcTemplate.update("CALL sp_daily_pay_slip_prepare(?)", slipExecutionId);
        Map<String, Object> preparedSlip = jdbcTemplate.queryForMap("""
                SELECT employee_address, work_hours_label,
                       legal_deposit_balance, loan_balance, saving_balance
                FROM daily_pay_slip_output
                WHERE execution_id = ?
                """, slipExecutionId);
        assertThat(preparedSlip.get("employee_address"))
                .isEqualTo("〒123-4567 東京都千代田区テスト1-2-3");
        assertThat(preparedSlip.get("work_hours_label")).isEqualTo("8時間");
        assertThat(preparedSlip).containsKeys(
                "legal_deposit_balance", "loan_balance", "saving_balance"
        );
        jdbcTemplate.update("CALL sp_daily_pay_slip_cleanup(?)", slipExecutionId);

        BigDecimal fallbackDeduction = jdbcTemplate.queryForObject("""
                SELECT item_value
                FROM vw_daily_pay_slip_item_source
                WHERE tenant_id = ?
                  AND payment_date = ?
                  AND employee_id = ?
                  AND item_code = 'OTHER_DEDUCTION'
                """, BigDecimal.class, TEST_TENANT_ID, workDate, dailyPaidDaily);
        assertAmount(fallbackDeduction, "1000");

        Long dailyReportId = jdbcTemplate.queryForObject("""
                SELECT id FROM daily_report
                WHERE tenant_id = ? AND employee_id = ? AND work_date = ?
                  AND deleted_at IS NULL
                """, Long.class, TEST_TENANT_ID, dailyPaidDaily, workDate);
        Map<String, Long> deductionMasterIds = new java.util.LinkedHashMap<>();
        deductionMasterIds.put("寮費", insertDailyStatementMaster(
                "DEDUCTION", "SLIP_DORMITORY_FEE", "寮費", 110));
        deductionMasterIds.put("携帯電話貸出料", insertDailyStatementMaster(
                "DEDUCTION", "SLIP_MOBILE_RENTAL", "携帯電話貸出料", 120));
        deductionMasterIds.put("Wi-Fi使用料", insertDailyStatementMaster(
                "DEDUCTION", "SLIP_WIFI_FEE", "Wi-Fi使用料", 130));
        deductionMasterIds.put("法定準備金", insertDailyStatementMaster(
                "DEDUCTION", "SLIP_LEGAL_DEPOSIT", "法定準備金", 160));
        Map<String, Integer> expectedAmounts = Map.of(
                "寮費", 1500,
                "携帯電話貸出料", 1000,
                "Wi-Fi使用料", 500,
                "法定準備金", 700
        );
        deductionMasterIds.forEach((name, masterId) -> {
            String code = switch (name) {
                case "寮費" -> "SLIP_DORMITORY_FEE";
                case "携帯電話貸出料" -> "SLIP_MOBILE_RENTAL";
                case "Wi-Fi使用料" -> "SLIP_WIFI_FEE";
                default -> "SLIP_LEGAL_DEPOSIT";
            };
            insertAllEmployeeDailyPolicy("DEDUCTION", masterId, code, name);
            int amount = expectedAmounts.get(name);
            jdbcTemplate.update("""
                    INSERT INTO daily_report_deductions (
                        daily_report_id, deduction_master_id,
                        deduction_code, deduction_name,
                        amount, calculated_amount, manual_override_flag,
                        quantity, balance_unit,
                        tenant_id, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, FALSE, ?, 'AMOUNT', ?,
                              CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                    """, dailyReportId, masterId, code, name,
                    amount, amount, amount, TEST_TENANT_ID);
        });
        jdbcTemplate.update("""
                UPDATE daily_report
                SET deduction_amount = 3700,
                    saving_amount = 800,
                    estimated_net_pay_amount = 7500,
                    updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """, dailyReportId);

        List<Map<String, Object>> configuredDeductions = jdbcTemplate.queryForList("""
                SELECT item_name, item_value
                FROM vw_daily_pay_slip_item_source
                WHERE tenant_id = ? AND employee_id = ? AND payment_date = ?
                  AND item_code IN (
                      'SLIP_DORMITORY_FEE', 'SLIP_MOBILE_RENTAL',
                      'SLIP_WIFI_FEE', 'SLIP_LEGAL_DEPOSIT', 'EMPLOYEE_SAVING'
                  )
                ORDER BY display_order, item_code
                """, TEST_TENANT_ID, dailyPaidDaily, workDate);
        assertThat(configuredDeductions).hasSize(5);
        Map<String, String> expectedStatementValues = Map.of(
                "寮費", "1500",
                "携帯電話貸出料", "1000",
                "Wi-Fi使用料", "500",
                "法定準備金", "700",
                "貯金", "800"
        );
        configuredDeductions.forEach(row -> assertAmount(
                row.get("item_value"),
                expectedStatementValues.get(row.get("item_name").toString())
        ));
        Map<String, Object> updatedSlip = jdbcTemplate.queryForMap("""
                SELECT deduction_total, net_payment_amount
                FROM vw_daily_pay_slip_latest
                WHERE tenant_id = ? AND employee_id = ? AND payment_date = ?
                """, TEST_TENANT_ID, dailyPaidDaily, workDate);
        assertAmount(updatedSlip.get("deduction_total"), "4500");
        assertAmount(updatedSlip.get("net_payment_amount"), "7500");

        List<Map<String, Object>> paymentDateRows = jdbcTemplate.queryForList("""
                SELECT *
                FROM vw_daily_labor_cost_preview
                WHERE tenant_id = ?
                  AND target_date = ?
                ORDER BY employee_code
                """, TEST_TENANT_ID, monthlyPaymentDate);
        assertThat(paymentDateRows).hasSize(2);
        assertThat(paymentDateRows)
                .allSatisfy(row -> assertAmount(row.get("gross_payment_amount"), "0"));
        assertAmount(paymentDateRows.get(0).get("payment_amount"), "14500");
        assertAmount(paymentDateRows.get(1).get("payment_amount"), "15000");
    }

    private Long insertDailyStatementMaster(
            String targetType,
            String targetCode,
            String displayName,
            int displayOrder
    ) {
        if ("ALLOWANCE".equals(targetType)) {
            jdbcTemplate.update("""
                    INSERT INTO allowance_masters (
                        allowance_code, allowance_name, allowance_type,
                        calculation_type, allowance_unit, detail_view_type,
                        taxable, show_on_daily_statement,
                        show_on_monthly_statement, display_order,
                        enabled, note, tenant_id, created_at, updated_at
                    ) VALUES (?, ?, 'COMPANY', 'MANUAL', 'DAILY', 'NONE',
                              TRUE, TRUE, FALSE, ?, TRUE,
                              '日次明細の動的表示テスト', ?,
                              CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                    """, targetCode, displayName, displayOrder, TEST_TENANT_ID);
            return jdbcTemplate.queryForObject("""
                    SELECT id FROM allowance_masters
                    WHERE tenant_id = ? AND allowance_code = ? AND deleted_at IS NULL
                    """, Long.class, TEST_TENANT_ID, targetCode);
        }

        jdbcTemplate.update("""
                INSERT INTO deduction_masters (
                    deduction_code, deduction_name, deduction_type,
                    calculation_type, default_amount, allow_manual_input,
                    deduction_unit, detail_view_type,
                    show_on_daily_statement, show_on_monthly_statement,
                    carry_to_monthly_settlement, display_order,
                    enabled, note, tenant_id, created_at, updated_at
                ) VALUES (?, ?, 'COMPANY', 'MANUAL', 0, TRUE,
                          'DAILY', 'NONE', TRUE, FALSE, FALSE, ?, TRUE,
                          '日次明細の動的表示テスト', ?,
                          CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, targetCode, displayName, displayOrder, TEST_TENANT_ID);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM deduction_masters
                WHERE tenant_id = ? AND deduction_code = ? AND deleted_at IS NULL
                """, Long.class, TEST_TENANT_ID, targetCode);
    }

    private void insertAllEmployeeDailyPolicy(
            String targetType,
            Long targetMasterId,
            String targetCode,
            String displayName
    ) {
        jdbcTemplate.update("""
                INSERT INTO payroll_item_balance_policy (
                    target_type, target_master_id, target_code, display_name,
                    application_scope, balance_unit, balance_tracking_flag,
                    input_source, accrual_frequency, accrual_rule_name,
                    carry_forward_flag, advance_consumption_flag, active_flag,
                    tenant_id, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'ALL_EMPLOYEES', 'AMOUNT', FALSE,
                          'DAILY_REPORT', 'MANUAL', 'NONE',
                          FALSE, FALSE, TRUE, ?,
                          CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, targetType, targetMasterId, targetCode, displayName, TEST_TENANT_ID);
    }

    private Long insertLaborCostEmployee(
            String employeeCode,
            String employeeName,
            String salaryType,
            String paymentCycle,
            String dailyWage,
            String weeklyWage,
            String monthlySalary
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
                """, employeeCode, employeeName, TEST_TENANT_ID);
        Long employeeId = jdbcTemplate.queryForObject("""
                SELECT id FROM employee
                WHERE tenant_id = ? AND employee_code = ?
                """, Long.class, TEST_TENANT_ID, employeeCode);
        assertThat(employeeId).isNotNull();

        jdbcTemplate.update("""
                INSERT INTO employee_contract (
                    employee_id, contract_start_date, renewal_flag,
                    salary_type, payment_cycle,
                    monthly_salary, weekly_wage, daily_wage, hourly_wage,
                    standard_working_hours,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, '2026-04-01', FALSE,
                    ?, ?,
                    ?, ?, ?, 0,
                    8,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeId, salaryType, paymentCycle,
                monthlySalary, weeklyWage, dailyWage, TEST_TENANT_ID);
        return employeeId;
    }

    private Long insertPaymentPreparationEmployee(
            String employeeCode,
            String employeeName,
            String paymentCycle
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
                """, employeeCode, employeeName, TEST_TENANT_ID);
        Long employeeId = jdbcTemplate.queryForObject("""
                SELECT id FROM employee
                WHERE tenant_id = ? AND employee_code = ?
                """, Long.class, TEST_TENANT_ID, employeeCode);
        assertThat(employeeId).isNotNull();

        jdbcTemplate.update("""
                INSERT INTO employee_contract (
                    employee_id, renewal_flag, salary_type, payment_cycle,
                    monthly_salary, weekly_wage, daily_wage, hourly_wage,
                    standard_working_hours,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, FALSE, 'DAILY', ?,
                    0, 0, 12000, 0, 8,
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, employeeId, paymentCycle, TEST_TENANT_ID);
        return employeeId;
    }

    private void insertPaymentPreparationReport(
            Long employeeId,
            LocalDate workDate,
            LocalDate paymentDate,
            String grossAmount,
            String allowanceAmount,
            String deductionAmount,
            String netAmount
    ) {
        jdbcTemplate.update("""
                INSERT INTO daily_report (
                    employee_id, work_date, payment_date,
                    normal_pay_amount, allowance_amount, deduction_amount,
                    saving_amount, loan_repayment_amount,
                    estimated_gross_pay_amount, estimated_net_pay_amount,
                    billing_base_unit_price, billing_overtime_unit_price,
                    billing_night_unit_price, billing_holiday_unit_price,
                    billing_commute_unit_price, holiday_premium_eligible,
                    dormitory_charge_days, overtime_pay_amount,
                    night_pay_amount, holiday_pay_amount,
                    work_hours,
                    vehicle_used_flag, work_description, approval_status,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, ?, ?,
                    ?, ?, ?, 0, 0, ?, ?,
                    0, 0, 0, 0, 0, FALSE,
                    0, 0, 0, 0, 8,
                    FALSE, 'Testcontainers日次給与明細備考', 'APPROVED',
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """,
                employeeId, workDate, paymentDate,
                grossAmount, allowanceAmount, deductionAmount,
                grossAmount, netAmount, TEST_TENANT_ID
        );
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
