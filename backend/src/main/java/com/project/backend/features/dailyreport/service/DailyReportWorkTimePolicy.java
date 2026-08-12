package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * V1の日報勤務時間区分を一か所で決定する。
 *
 * <p>休日割増の対象日は曜日から決めず、日報入力時に明示された
 * holidayPremiumEligibleを使用する。</p>
 */
public final class DailyReportWorkTimePolicy {

    private DailyReportWorkTimePolicy() {
    }

    public static WorkTimes resolve(
            LocalDate workDate,
            BigDecimal workHours,
            BigDecimal overtimeHours,
            BigDecimal nightWorkHours,
            BigDecimal holidayWorkHours,
            boolean holidayPremiumEligible
    ) {
        BigDecimal normal = nvl(workHours);
        BigDecimal overtime = nvl(overtimeHours);
        BigDecimal night = nvl(nightWorkHours);
        BigDecimal holiday = nvl(holidayWorkHours);

        if (!holidayPremiumEligible) {
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
