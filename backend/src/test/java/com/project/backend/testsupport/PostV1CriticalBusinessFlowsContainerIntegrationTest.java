package com.project.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.MySQLContainer;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.customer.dto.CustomerPaymentConfirmRequest;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerTransactionRepository;
import com.project.backend.features.customer.service.CustomerTransactionCommandService;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingItemStatus;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingItemRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.operation.monthly.service.MonthlyClosingCustomerTransactionService;
import com.project.backend.features.operation.monthly.service.MonthlyClosingExecutionStateService;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostV1CriticalBusinessFlowsContainerIntegrationTest
        extends ContainerIntegrationTest {

    private static final String PAY_SLIP_EXECUTION =
            "POSTV1-PAY-SLIP-2026-07-V1";
    private static final String LABOR_COST_EXECUTION =
            "POSTV1-LABOR-COST-2026-07-V1";

    @Autowired
    private MySQLContainer<?> mysqlContainer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerTransactionRepository transactionRepository;

    @Autowired
    private MonthlyClosingCustomerTransactionService transactionSyncService;

    @Autowired
    private CustomerTransactionCommandService transactionCommandService;

    @Autowired
    private MonthlyClosingExecutionStateService executionStateService;

    @Autowired
    private MonthlyClosingRepository closingRepository;

    @Autowired
    private MonthlyClosingItemRepository closingItemRepository;

    private PostV1PayrollScenarioFixture.Scenario scenario;

    @BeforeAll
    void installAssetsAndCreateSharedScenario() throws Exception {
        TenantContext.setTenantId(TEST_TENANT_ID);
        RuntimeSchemaAssetInstaller.apply(
                mysqlContainer,
                RuntimeSchemaAssetInstaller.readManifest()
        );
        scenario = new PostV1PayrollScenarioFixture(
                jdbcTemplate,
                TEST_TENANT_ID
        ).create();
        TenantContext.clear();
    }

    @Test
    void threePaymentCyclesShareOneJulyFixtureWithExpectedPaymentDates() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT employee.employee_code,
                       contract.payment_cycle,
                       report.work_date,
                       report.payment_date,
                       report.estimated_gross_pay_amount
                FROM daily_report report
                JOIN employee employee
                  ON employee.id = report.employee_id
                 AND employee.tenant_id = report.tenant_id
                JOIN employee_contract contract
                  ON contract.employee_id = employee.id
                 AND contract.tenant_id = employee.tenant_id
                 AND contract.deleted_at IS NULL
                WHERE report.tenant_id = ?
                  AND report.work_date BETWEEN '2026-07-01' AND '2026-07-31'
                  AND report.deleted_at IS NULL
                ORDER BY employee.employee_code, report.work_date
                """, TEST_TENANT_ID);

        assertThat(rows).hasSize(5);
        assertThat(rows).extracting(row -> row.get("payment_cycle"))
                .containsExactly("DAILY", "MONTHLY", "MONTHLY", "WEEKLY", "WEEKLY");
        assertThat(paymentDates(rows, "POSTV1-DAILY-001"))
                .containsOnly(PostV1PayrollScenarioFixture.DAILY_PAYMENT_DATE);
        assertThat(paymentDates(rows, "POSTV1-WEEKLY-001"))
                .containsOnly(PostV1PayrollScenarioFixture.WEEKLY_PAYMENT_DATE);
        assertThat(paymentDates(rows, "POSTV1-MONTHLY-001"))
                .containsOnly(PostV1PayrollScenarioFixture.MONTHLY_PAYMENT_DATE);
        assertThat(sum(rows, "estimated_gross_pay_amount"))
                .isEqualByComparingTo("66000");
    }

    @Test
    void dailyReportsSnapshotIntoMonthlyPaySlipsAndLaborCostList() {
        var closing = closingRepository
                .findByTargetMonthAndDeletedAtIsNull(
                        PostV1PayrollScenarioFixture.TARGET_MONTH
                )
                .orElseThrow();
        var execution = executionStateService.startNew(
                closing.getId(),
                1,
                "post-v1-integration",
                List.of(
                        outputDefinition("MONTHLY_PAY_SLIP"),
                        outputDefinition("MONTHLY_LABOR_COST_LIST")
                )
        );

        jdbcTemplate.update("""
                INSERT INTO monthly_pay_slip_input (
                    execution_id, target_month, employee_id,
                    closing_version, execution_mode,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, '2026-07', NULL,
                    1, 'INITIAL',
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, PAY_SLIP_EXECUTION, TEST_TENANT_ID);
        jdbcTemplate.execute(
                "CALL sp_monthly_pay_slip_snapshot('" + PAY_SLIP_EXECUTION + "')"
        );

        jdbcTemplate.update("""
                INSERT INTO monthly_labor_cost_list_input (
                    execution_id, target_month,
                    closing_version, execution_mode,
                    tenant_id, created_at, updated_at
                ) VALUES (
                    ?, '2026-07',
                    1, 'INITIAL',
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, LABOR_COST_EXECUTION, TEST_TENANT_ID);
        jdbcTemplate.execute(
                "CALL sp_monthly_labor_cost_list_snapshot('"
                        + LABOR_COST_EXECUTION
                        + "')"
        );

        assertSnapshotTotals(
                "monthly_pay_slip_history",
                "gross_amount"
        );
        assertSnapshotTotals(
                "monthly_labor_cost_list_history",
                "gross_amount"
        );

        var items = closingItemRepository
                .findByMonthlyClosingExecutionIdAndDeletedAtIsNullOrderByIdAsc(
                        execution.getId()
                );
        assertThat(items).hasSize(2);
        items.forEach(item -> {
            item.setStatus(MonthlyClosingItemStatus.COMPLETED);
            item.setHistoryRowCount(3L);
            item.setCompletedAt(testClock.instant());
        });
        closingItemRepository.saveAllAndFlush(items);
        executionStateService.complete(execution.getId());

        var completedClosing = closingRepository
                .findById(closing.getId())
                .orElseThrow();
        assertThat(completedClosing.getStatus())
                .isEqualTo(MonthlyClosingStatus.CLOSED);
        assertThat(completedClosing.getClosingVersion()).isEqualTo(1);
    }

    @Test
    void customerClosingCreatesTransactionAndReceiptConfirmationRow() {
        Customer customer = new Customer();
        customer.setName("V1後顧客締め回帰顧客");
        customer.setClosingDayType(DayRuleType.END_OF_MONTH);
        customer.setClosingMonthOffset(0);
        customer.setPaymentDayType(DayRuleType.DAY_OF_MONTH);
        customer.setPaymentDayValue(25);
        customer.setPaymentMonthOffset(1);
        customer = customerRepository.saveAndFlush(customer);

        long historyId = insertInvoiceHistory(customer.getId(), "110000");
        assertThat(transactionSyncService.synchronize(
                "2026-07", 1, customer.getId()
        )).isEqualTo(1);

        var transaction = transactionRepository
                .findByCustomerIdAndTargetMonthAndDeletedAtIsNull(
                        customer.getId(), "2026-07"
                )
                .orElseThrow();
        assertThat(transaction.getSourceInvoiceHistoryId()).isEqualTo(historyId);
        assertThat(transaction.getBillingAmount()).isEqualTo(110_000);
        assertThat(transaction.getExpectedPaymentDate())
                .isEqualTo(LocalDate.of(2026, 8, 25));

        transactionCommandService.confirmPaymentFromLedger(
                customer.getId(),
                transaction.getId(),
                "2026-07",
                new CustomerPaymentConfirmRequest(
                        LocalDate.of(2026, 8, 25),
                        109_450,
                        550,
                        0,
                        0,
                        "V1後入金確認表回帰"
                )
        );

        Map<String, Object> receipt = jdbcTemplate.queryForMap("""
                SELECT billing_amount, expected_payment_date,
                       paid_amount, fee, settled_amount,
                       remaining_amount, payment_status
                FROM vw_receipt_confirmation_ledger
                WHERE tenant_id = ?
                  AND target_month = '2026-07'
                  AND customer_id = ?
                """, TEST_TENANT_ID, customer.getId());
        assertAmount(receipt.get("billing_amount"), "110000");
        assertThat(receipt.get("expected_payment_date"))
                .isEqualTo(java.sql.Date.valueOf("2026-08-25"));
        assertAmount(receipt.get("paid_amount"), "109450");
        assertAmount(receipt.get("fee"), "550");
        assertAmount(receipt.get("settled_amount"), "110000");
        assertAmount(receipt.get("remaining_amount"), "0");
        assertThat(receipt.get("payment_status")).isEqualTo("PAID");
    }

    @Test
    void paymentPreparationAggregatesByActualPaymentDateAndCycle() {
        assertPaymentPreparation(
                PostV1PayrollScenarioFixture.DAILY_PAYMENT_DATE,
                "DAILY",
                "12000"
        );
        assertPaymentPreparation(
                PostV1PayrollScenarioFixture.WEEKLY_PAYMENT_DATE,
                "WEEKLY",
                "26000"
        );
        Map<String, Object> monthly = assertPaymentPreparation(
                PostV1PayrollScenarioFixture.MONTHLY_PAYMENT_DATE,
                "MONTHLY",
                "28000"
        );

        assertAmount(monthly.get("total_net_payment_amount"), "28000");
        assertAmount(monthly.get("bank_withdrawal_amount"), "28000");
        assertAmount(monthly.get("bill_10000"), "2");
        assertAmount(monthly.get("bill_5000"), "1");
        assertAmount(monthly.get("bill_1000"), "3");
    }

    private List<LocalDate> paymentDates(
            List<Map<String, Object>> rows,
            String employeeCode
    ) {
        return rows.stream()
                .filter(row -> employeeCode.equals(row.get("employee_code")))
                .map(row -> ((java.sql.Date) row.get("payment_date")).toLocalDate())
                .toList();
    }

    private BigDecimal sum(List<Map<String, Object>> rows, String column) {
        return rows.stream()
                .map(row -> new BigDecimal(row.get(column).toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertSnapshotTotals(String table, String amountColumn) {
        Map<String, Object> result = jdbcTemplate.queryForMap("""
                SELECT COUNT(*) AS row_count,
                       COALESCE(SUM(%s), 0) AS total_amount
                FROM %s
                WHERE tenant_id = ?
                  AND target_month = '2026-07-01'
                  AND closing_version = 1
                  AND deleted_at IS NULL
                """.formatted(amountColumn, table), TEST_TENANT_ID);
        assertThat(((Number) result.get("row_count")).intValue()).isEqualTo(3);
        assertAmount(result.get("total_amount"), "66000");
    }

    private long insertInvoiceHistory(Long customerId, String total) {
        BigDecimal totalAmount = new BigDecimal(total);
        BigDecimal subtotal = totalAmount.divide(
                new BigDecimal("1.1"), 2, RoundingMode.HALF_UP
        );
        String businessKey = "POSTV1-INVOICE-2026-07-V1-" + customerId;
        jdbcTemplate.update("""
                INSERT INTO monthly_invoice_history (
                    target_month, period_from, period_to, closing_version,
                    customer_id, customer_name, invoice_type,
                    invoice_number, issue_date,
                    company_name, tax_rate,
                    subtotal_amount, tax_amount, total_amount,
                    business_key, source_execution_id, fixed_at,
                    tenant_id, created_at, updated_at, deleted_at
                ) VALUES (
                    '2026-07-01', '2026-07-01', '2026-07-31', 1,
                    ?, 'V1後顧客締め回帰顧客', 'PATTERN_1',
                    'POSTV1-INV-202607-V1', '2026-08-01',
                    'V1後回帰テスト会社', 0.1000,
                    ?, ?, ?,
                    ?, 'POSTV1-CUSTOMER-CLOSING-V1', CURRENT_TIMESTAMP(6),
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
                )
                """,
                customerId,
                subtotal,
                totalAmount.subtract(subtotal),
                totalAmount,
                businessKey,
                TEST_TENANT_ID);
        Long id = jdbcTemplate.queryForObject("""
                SELECT id
                FROM monthly_invoice_history
                WHERE tenant_id = ? AND business_key = ?
                """, Long.class, TEST_TENANT_ID, businessKey);
        if (id == null) {
            throw new IllegalStateException("invoice history was not created");
        }
        return id;
    }

    private MonthlyClosingOutputDefinition outputDefinition(String code) {
        MonthlyClosingOutputDefinition definition =
                new MonthlyClosingOutputDefinition();
        definition.setOutputType(MonthlyClosingOutputType.REPORT);
        definition.setOutputCode(code);
        definition.setRequiredFlag(true);
        definition.setActiveFlag(true);
        return definition;
    }

    private Map<String, Object> assertPaymentPreparation(
            LocalDate paymentDate,
            String expectedCycle,
            String expectedAmount
    ) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT *
                FROM vw_daily_payment_preparation_preview
                WHERE tenant_id = ? AND target_date = ?
                ORDER BY payment_cycle_order, employee_code
                """, TEST_TENANT_ID, paymentDate);
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("payment_cycle")).isEqualTo(expectedCycle);
            assertAmount(row.get("gross_payment_amount"), expectedAmount);
            assertAmount(row.get("payment_amount"), expectedAmount);
        });
        return rows.getFirst();
    }

    private void assertAmount(Object actual, String expected) {
        assertThat(new BigDecimal(actual.toString()))
                .isEqualByComparingTo(expected);
    }
}
