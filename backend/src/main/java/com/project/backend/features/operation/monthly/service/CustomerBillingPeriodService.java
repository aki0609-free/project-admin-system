package com.project.backend.features.operation.monthly.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.common.dayrule.utils.DayRuleUtils;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.operation.monthly.dto.CustomerBillingPeriod;
import com.project.backend.features.operation.monthly.utils.MonthlyOperationDateUtil;

/**
 * 顧客マスターの締日から請求対象期間を計算する。
 */
@Service
public class CustomerBillingPeriodService {

    public CustomerBillingPeriod resolve(
            String targetMonthText,
            Customer customer
    ) {
        if (customer == null) {
            throw new IllegalArgumentException("customerは必須です。");
        }
        if (customer.getClosingDayType() == null) {
            throw new IllegalStateException(
                    "顧客の締日区分が未設定です。customerId=" + customer.getId()
            );
        }

        YearMonth targetMonth =
                MonthlyOperationDateUtil.parseTargetMonth(targetMonthText);
        int monthOffset = customer.getClosingMonthOffset() == null
                ? 0
                : customer.getClosingMonthOffset();
        DayRule rule = DayRule.builder()
                .type(customer.getClosingDayType())
                .value(customer.getClosingDayValue())
                .monthOffset(monthOffset)
                .build();

        LocalDate endDate = DayRuleUtils.resolve(rule, targetMonth);
        if (endDate == null) {
            throw new IllegalStateException(
                    "顧客の締日設定から請求終了日を計算できません。customerId="
                            + customer.getId()
            );
        }

        YearMonth previousClosingMonth =
                targetMonth.plusMonths(monthOffset).minusMonths(1);
        LocalDate previousEndDate = DayRuleUtils.resolve(
                customer.getClosingDayType(),
                customer.getClosingDayValue(),
                previousClosingMonth
        );

        return new CustomerBillingPeriod(
                targetMonthText,
                previousEndDate.plusDays(1),
                endDate,
                rule
        );
    }
}
