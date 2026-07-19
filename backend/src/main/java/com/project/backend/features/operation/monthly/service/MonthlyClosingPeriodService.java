package com.project.backend.features.operation.monthly.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.project.backend.common.closing.service.ClosingSettingQueryService;
import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.utils.MonthlyOperationDateUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonthlyClosingPeriodService {

    private final ClosingSettingQueryService closingSettingQueryService;

    public MonthlyClosingPeriod resolve(String targetMonthText) {
        YearMonth targetMonth =
                MonthlyOperationDateUtil.parseTargetMonth(targetMonthText);

        DayRule rule =
                closingSettingQueryService.getPayrollClosingDayRule();

        if (rule.type() == DayRuleType.END_OF_MONTH) {
            return new MonthlyClosingPeriod(
                    targetMonth.toString(),
                    targetMonth.atDay(1),
                    targetMonth.atEndOfMonth(),
                    rule
            );
        }

        if (rule.type() == DayRuleType.DAY_OF_MONTH) {
            int closingDay = rule.value() != null ? rule.value() : 31;

            YearMonth previousMonth = targetMonth.minusMonths(1);

            LocalDate previousClosingDate =
                    previousMonth.atDay(
                            Math.min(closingDay, previousMonth.lengthOfMonth())
                    );

            LocalDate startDate =
                    previousClosingDate.plusDays(1);

            LocalDate endDate =
                    targetMonth.atDay(
                            Math.min(closingDay, targetMonth.lengthOfMonth())
                    );

            return new MonthlyClosingPeriod(
                    targetMonth.toString(),
                    startDate,
                    endDate,
                    rule
            );
        }

        throw new RuntimeException("未対応の締日区分です: " + rule.type());
    }
}