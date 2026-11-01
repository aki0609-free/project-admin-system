package com.project.backend.features.system.calendar.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.project.backend.features.system.calendar.enums.CalendarType;

/**
 * 官公庁・金融機関カレンダーに組み込まれている固定ルールの共通定義。
 */
public final class CalendarFixedRuleCatalog {
    private CalendarFixedRuleCatalog() {}

    public static Set<DayOfWeek> closedWeekdays(CalendarType type) {
        return switch (type) {
            case GOVERNMENT, FINANCIAL_INSTITUTION ->
                    EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
            case NATIONAL_HOLIDAY, COMPANY -> Set.of();
        };
    }

    public static List<FixedPeriod> fixedPeriods(CalendarType type) {
        return switch (type) {
            case GOVERNMENT -> List.of(new FixedPeriod(
                    "年末年始", MonthDay.of(12, 29), MonthDay.of(1, 3)
            ));
            case FINANCIAL_INSTITUTION -> List.of(new FixedPeriod(
                    "年末年始", MonthDay.of(12, 31), MonthDay.of(1, 3)
            ));
            case NATIONAL_HOLIDAY, COMPANY -> List.of();
        };
    }

    public static boolean isClosedByFixedRule(
            CalendarType type,
            LocalDate date
    ) {
        if (closedWeekdays(type).contains(date.getDayOfWeek())) return true;
        return fixedPeriods(type).stream().anyMatch(period -> period.contains(date));
    }

    public record FixedPeriod(
            String name,
            MonthDay start,
            MonthDay end
    ) {
        public boolean contains(LocalDate date) {
            MonthDay target = MonthDay.from(date);
            if (start.compareTo(end) <= 0) {
                return target.compareTo(start) >= 0 && target.compareTo(end) <= 0;
            }
            return target.compareTo(start) >= 0 || target.compareTo(end) <= 0;
        }
    }
}
