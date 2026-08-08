package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * V1の日報勤務時間区分を一か所で決定する。
 *
 * <p>土曜・日曜の勤務は休日勤務として扱う。会社カレンダー対応時は、
 * このポリシーの判定元だけを差し替える。</p>
 */
public final class DailyReportWorkTimePolicy {

    private DailyReportWorkTimePolicy() {
    }

    public static WorkTimes resolve(
            LocalDate workDate,
            BigDecimal workHours,
            BigDecimal overtimeHours,
            BigDecimal nightWorkHours,
            BigDecimal holidayWorkHours
    ) {
        BigDecimal normal = nvl(workHours);
        BigDecimal overtime = nvl(overtimeHours);
        BigDecimal night = nvl(nightWorkHours);
        BigDecimal holiday = nvl(holidayWorkHours);

        if (!isWeekend(workDate)) {
            return new WorkTimes(normal, overtime, night, holiday);
        }

        BigDecimal calculatedHoliday = normal.add(overtime);
        return new WorkTimes(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                night,
                holiday.max(calculatedHoliday)
        );
    }

    public static boolean isWeekend(LocalDate workDate) {
        if (workDate == null) {
            return false;
        }
        DayOfWeek dayOfWeek = workDate.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY
                || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record WorkTimes(
            BigDecimal workHours,
            BigDecimal overtimeHours,
            BigDecimal nightWorkHours,
            BigDecimal holidayWorkHours
    ) {
    }
}
