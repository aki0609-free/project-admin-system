package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.enums.CustomerPaymentStatus;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerTransactionRepository;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingExecution;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingExecutionStatus;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingItemStatus;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingExecutionRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingItemRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.system.report.invoice.repository.MonthlyInvoiceHistoryRepository;
import com.project.backend.testsupport.ContainerIntegrationTest;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MonthlyClosingContainerIntegrationTest
        extends ContainerIntegrationTest {

    @Autowired
    private MonthlyClosingExecutionStateService executionStateService;

    @Autowired
    private MonthlyClosingCustomerTransactionService transactionService;

    @Autowired
    private MonthlyClosingRepository closingRepository;

    @Autowired
    private MonthlyClosingExecutionRepository executionRepository;

    @Autowired
    private MonthlyClosingItemRepository itemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerTransactionRepository transactionRepository;

    @Autowired
    private MonthlyInvoiceHistoryRepository invoiceHistoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void firstClosingAndReclosingCreateImmutableExecutionVersions() {
        MonthlyClosing closing = new MonthlyClosing();
        closing.setTargetMonth(LocalDate.of(2026, 8, 1));
        closing.setClosingStartDate(LocalDate.of(2026, 8, 1));
        closing.setClosingEndDate(LocalDate.of(2026, 8, 31));
        closing.setStatus(MonthlyClosingStatus.OPEN);
        closing.setClosingVersion(0);
        closing = closingRepository.save(closing);
        Long closingId = closing.getId();

        List<MonthlyClosingOutputDefinition> definitions = List.of(
                outputDefinition(
                        MonthlyClosingOutputType.REPORT,
                        "MONTHLY_PAY_SLIP"
                ),
                outputDefinition(
                        MonthlyClosingOutputType.LEDGER,
                        "MONTHLY_LABOR_LEDGER"
                )
        );

        testClock.setDate(LocalDate.of(2026, 9, 1));
        MonthlyClosingExecution version1 = executionStateService.startNew(
                closingId,
                1,
                "integration-admin",
                definitions
        );
        completeItems(version1);
        executionStateService.complete(version1.getId());

        testClock.setDate(LocalDate.of(2026, 9, 2));
        MonthlyClosingExecution version2 = executionStateService.startNew(
                closingId,
                2,
                "integration-admin",
                definitions
        );
        completeItems(version2);
        executionStateService.complete(version2.getId());

        MonthlyClosing savedClosing = closingRepository
                .findById(closingId)
                .orElseThrow();
        assertThat(savedClosing.getStatus())
                .isEqualTo(MonthlyClosingStatus.CLOSED);
        assertThat(savedClosing.getClosingVersion()).isEqualTo(2);
        assertThat(savedClosing.getClosedAt()).isEqualTo(testClock.instant());
        assertThat(savedClosing.getClosedBy()).isEqualTo("integration-admin");

        List<MonthlyClosingExecution> executions = executionRepository
                .findByMonthlyClosingIdAndDeletedAtIsNullOrderByClosingVersionDesc(
                        closingId
                );
        assertThat(executions)
                .extracting(MonthlyClosingExecution::getClosingVersion)
                .containsExactly(2, 1);
        assertThat(executions)
                .extracting(MonthlyClosingExecution::getStatus)
                .containsOnly(MonthlyClosingExecutionStatus.COMPLETED);

        assertThatThrownBy(() -> executionStateService.startNew(
                closingId,
                2,
                "integration-admin",
                definitions
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Version")
                .hasMessageContaining("既に存在");
    }

    @Test
    void reclosedInvoiceHistoryUpdatesOneCustomerTransactionAndKeepsOldHistory() {
        Customer customer = new Customer();
        customer.setName("月次締め統合テスト顧客");
        customer.setClosingDayType(DayRuleType.END_OF_MONTH);
        customer.setClosingMonthOffset(0);
        customer.setPaymentDayType(DayRuleType.DAY_OF_MONTH);
        customer.setPaymentDayValue(25);
        customer.setPaymentMonthOffset(1);
        customer = customerRepository.save(customer);

        long version1HistoryId = insertInvoiceHistory(
                customer.getId(),
                1,
                new BigDecimal("100000")
        );

        assertThat(transactionService.synchronize("2026-08", 1))
                .isEqualTo(1);
        var version1Transaction = transactionRepository
                .findByCustomerIdAndTargetMonthAndDeletedAtIsNull(
                        customer.getId(),
                        "2026-08"
                )
                .orElseThrow();
        Long transactionId = version1Transaction.getId();
        assertThat(version1Transaction.getBillingAmount()).isEqualTo(100_000);
        assertThat(version1Transaction.getExpectedPaymentDate())
                .isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(version1Transaction.getSourceType())
                .isEqualTo("MONTHLY_CLOSING");
        assertThat(version1Transaction.getSourceInvoiceHistoryId())
                .isEqualTo(version1HistoryId);
        assertThat(version1Transaction.getSourceClosingVersion()).isEqualTo(1);
        assertThat(version1Transaction.getPaymentStatus())
                .isEqualTo(CustomerPaymentStatus.UNPAID);

        long version2HistoryId = insertInvoiceHistory(
                customer.getId(),
                2,
                new BigDecimal("120000")
        );

        assertThat(transactionService.synchronize("2026-08", 2))
                .isEqualTo(1);
        var version2Transaction = transactionRepository
                .findByCustomerIdAndTargetMonthAndDeletedAtIsNull(
                        customer.getId(),
                        "2026-08"
                )
                .orElseThrow();

        assertThat(version2Transaction.getId()).isEqualTo(transactionId);
        assertThat(version2Transaction.getBillingAmount()).isEqualTo(120_000);
        assertThat(version2Transaction.getSourceInvoiceHistoryId())
                .isEqualTo(version2HistoryId);
        assertThat(version2Transaction.getSourceClosingVersion()).isEqualTo(2);
        assertThat(transactionRepository
                .findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(customer.getId()))
                .hasSize(1);
        assertThat(invoiceHistoryRepository
                .findByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByCustomerIdAsc(
                        LocalDate.of(2026, 8, 1),
                        1
                )).hasSize(1);
        assertThat(invoiceHistoryRepository
                .findByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByCustomerIdAsc(
                        LocalDate.of(2026, 8, 1),
                        2
                )).hasSize(1);
    }

    private MonthlyClosingOutputDefinition outputDefinition(
            MonthlyClosingOutputType type,
            String code
    ) {
        MonthlyClosingOutputDefinition definition =
                new MonthlyClosingOutputDefinition();
        definition.setOutputType(type);
        definition.setOutputCode(code);
        definition.setRequiredFlag(true);
        definition.setActiveFlag(true);
        return definition;
    }

    private void completeItems(MonthlyClosingExecution execution) {
        var items = itemRepository
                .findByMonthlyClosingExecutionIdAndDeletedAtIsNullOrderByIdAsc(
                        execution.getId()
                );
        assertThat(items).hasSize(2);
        items.forEach(item -> {
            item.setStatus(MonthlyClosingItemStatus.COMPLETED);
            item.setHistoryRowCount(1L);
            item.setCompletedAt(testClock.instant());
        });
        itemRepository.saveAll(items);
    }

    private long insertInvoiceHistory(
            Long customerId,
            int closingVersion,
            BigDecimal totalAmount
    ) {
        String businessKey = "INTEGRATION-INVOICE-2026-08-V"
                + closingVersion + "-" + customerId;
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
                    ?, ?, ?, ?,
                    ?, ?, 'PATTERN_1',
                    ?, ?,
                    '統合テスト会社', 0.1000,
                    ?, ?, ?,
                    ?, ?, CURRENT_TIMESTAMP(6),
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL
                )
                """,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                closingVersion,
                customerId,
                "月次締め統合テスト顧客",
                "INV-202608-V" + closingVersion,
                LocalDate.of(2026, 9, 1),
                totalAmount.divide(new BigDecimal("1.1"), 2,
                        java.math.RoundingMode.HALF_UP),
                totalAmount.subtract(totalAmount.divide(
                        new BigDecimal("1.1"), 2,
                        java.math.RoundingMode.HALF_UP
                )),
                totalAmount,
                businessKey,
                "INTEGRATION-CLOSING-V" + closingVersion,
                TEST_TENANT_ID
        );
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM monthly_invoice_history WHERE business_key = ?",
                Long.class,
                businessKey
        );
        return id == null ? 0L : id;
    }
}
