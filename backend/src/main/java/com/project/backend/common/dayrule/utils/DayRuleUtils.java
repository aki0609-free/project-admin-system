package com.project.backend.common.dayrule.utils;

import java.time.LocalDate;
import java.time.YearMonth;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.dto.DayRuleResponse;
import com.project.backend.common.dayrule.enums.DayRuleType;

public class DayRuleUtils {

    private DayRuleUtils() {
    }

    public static LocalDate resolve(
            DayRuleType type,
            Integer value,
            YearMonth yearMonth
    ) {
        return resolve(type, value, 0, yearMonth);
    }

    public static LocalDate resolve(
            DayRuleType type,
            Integer value,
            Integer monthOffset,
            YearMonth yearMonth
    ) {
        if (type == null || yearMonth == null) {
            return null;
        }

        YearMonth resolvedMonth = yearMonth.plusMonths(monthOffset == null ? 0 : monthOffset);

        return switch (type) {
            case DAY_OF_MONTH -> {
                if (value == null) {
                    yield null;
                }

                int maxDay = resolvedMonth.lengthOfMonth();
                int safeDay = Math.min(value, maxDay);

                yield resolvedMonth.atDay(safeDay);
            }

            case END_OF_MONTH -> resolvedMonth.atEndOfMonth();
        };
    }

    public static LocalDate resolve(
            DayRule rule,
            YearMonth yearMonth
    ) {
        if (rule == null) {
            return null;
        }

        return resolve(rule.type(), rule.value(), rule.monthOffset(), yearMonth);
    }

    public static String toLabel(
            DayRuleType type,
            Integer value
    ) {
        return toLabel(type, value, 0);
    }

    public static String toLabel(
            DayRuleType type,
            Integer value,
            Integer monthOffset
    ) {
        if (type == null) {
            return "";
        }

        String prefix = switch (monthOffset == null ? 0 : monthOffset) {
            case 0 -> "当月";
            case 1 -> "翌月";
            case 2 -> "翌々月";
            default -> (monthOffset + "ヶ月後");
        };

        String dayLabel = switch (type) {
            case DAY_OF_MONTH -> value == null ? "" : value + "日";
            case END_OF_MONTH -> "末日";
        };

        return prefix + dayLabel;
    }

    public static DayRuleResponse toResponse(
            DayRuleType type,
            Integer value
    ) {
        return toResponse(type, value, 0);
    }

    public static DayRuleResponse toResponse(
            DayRuleType type,
            Integer value,
            Integer monthOffset
    ) {
        return DayRuleResponse.builder()
                .type(type)
                .value(value)
                .monthOffset(monthOffset == null ? 0 : monthOffset)
                .label(toLabel(type, value, monthOffset))
                .build();
    }
}