package com.project.backend.features.operation.monthly.service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.utils.DayRuleUtils;
import com.project.backend.features.customer.dto.CustomerTransactionClosingRequest;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.service.CustomerTransactionCommandService;
import com.project.backend.features.system.report.invoice.entity.MonthlyInvoiceHistory;
import com.project.backend.features.system.report.invoice.repository.MonthlyInvoiceHistoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * 確定請求履歴を顧客取引へ同期する月次締め処理。
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MonthlyClosingCustomerTransactionService {

    private final MonthlyInvoiceHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTransactionCommandService commandService;

    public int synchronize(
            String targetMonthText,
            Integer closingVersion
    ) {
        YearMonth targetMonth = YearMonth.parse(targetMonthText);
        if (closingVersion == null || closingVersion < 1) {
            throw new IllegalArgumentException(
                    "closingVersionは1以上で指定してください。"
            );
        }

        List<MonthlyInvoiceHistory> histories = historyRepository
                .findByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByCustomerIdAsc(
                        targetMonth.atDay(1),
                        closingVersion
                );
        if (histories.isEmpty()) {
            throw new IllegalStateException(
                    "顧客取引へ同期する確定請求履歴がありません。targetMonth="
                            + targetMonthText
                            + ", closingVersion="
                            + closingVersion
            );
        }

        for (MonthlyInvoiceHistory history : histories) {
            Customer customer = customerRepository
                    .findById(history.getCustomerId())
                    .orElseThrow(() -> new IllegalStateException(
                            "確定請求履歴の顧客が見つかりません。customerId="
                                    + history.getCustomerId()
                    ));
            DayRule closingRule = closingRule(customer);
            DayRule paymentRule = paymentRule(customer);
            LocalDate expectedPaymentDate = DayRuleUtils.resolve(
                    paymentRule,
                    targetMonth
            );

            commandService.upsertFromMonthlyClosing(
                    new CustomerTransactionClosingRequest(
                            customer.getId(),
                            targetMonthText,
                            closingRule,
                            paymentRule,
                            exactYen(history),
                            expectedPaymentDate,
                            null,
                            history.getId(),
                            closingVersion
                    )
            );
        }
        return histories.size();
    }

    private int exactYen(MonthlyInvoiceHistory history) {
        try {
            return history.getTotalAmount()
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "確定請求額を円単位の整数へ変換できません。invoiceHistoryId="
                            + history.getId(),
                    exception
            );
        }
    }

    private DayRule closingRule(Customer customer) {
        return DayRule.builder()
                .type(customer.getClosingDayType())
                .value(customer.getClosingDayValue())
                .monthOffset(customer.getClosingMonthOffset())
                .build();
    }

    private DayRule paymentRule(Customer customer) {
        return DayRule.builder()
                .type(customer.getPaymentDayType())
                .value(customer.getPaymentDayValue())
                .monthOffset(customer.getPaymentMonthOffset())
                .build();
    }
}
