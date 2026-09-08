package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;

/** 日報の開始・終了・休憩から、保存する勤務時間区分を一意に計算する。 */
@Component
public class DailyReportWorkTimeCalculator {

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final int STANDARD_WORK_MINUTES = 8 * 60;
    private static final int NIGHT_START_MINUTES = 22 * 60;
    private static final int NIGHT_END_MINUTES = 5 * 60;

    public DailyReportWorkTimePolicy.WorkTimes calculate(
            DailyReportSaveRequest request
    ) {
        LocalTime start = request.startTime();
        LocalTime end = request.endTime();

        if (start == null && end == null) {
            ensureNoWorkHoursWithoutTime(request);
            return DailyReportWorkTimePolicy.resolve(
                    request.workDate(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    Boolean.TRUE.equals(request.holidayPremiumEligible())
            );
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("開始時刻と終了時刻は両方入力してください。");
        }
        if (start.equals(end)) {
            throw new IllegalArgumentException("開始時刻と終了時刻を同じ時刻にはできません。");
        }

        int elapsedMinutes = elapsedMinutes(start, end);
        int breakMinutes = request.breakMinutes() != null
                ? request.breakMinutes()
                : 0;
        if (breakMinutes > elapsedMinutes) {
            throw new IllegalArgumentException("休憩時間は勤務区間を超えて指定できません。");
        }

        int effectiveMinutes = elapsedMinutes - breakMinutes;
        int normalMinutes = Math.min(effectiveMinutes, STANDARD_WORK_MINUTES);
        int overtimeMinutes = Math.max(effectiveMinutes - STANDARD_WORK_MINUTES, 0);
        int nightMinutes = calculateNightMinutes(start, elapsedMinutes);
        boolean holiday = Boolean.TRUE.equals(request.holidayPremiumEligible());

        return new DailyReportWorkTimePolicy.WorkTimes(
                holiday ? BigDecimal.ZERO : toHours(normalMinutes),
                holiday ? BigDecimal.ZERO : toHours(overtimeMinutes),
                toHours(nightMinutes),
                holiday ? toHours(effectiveMinutes) : BigDecimal.ZERO
        );
    }

    private void ensureNoWorkHoursWithoutTime(DailyReportSaveRequest request) {
        if (positive(request.workHours())
                || positive(request.overtimeHours())
                || positive(request.nightWorkHours())
                || positive(request.holidayWorkHours())) {
            throw new IllegalArgumentException(
                    "勤務時間を登録する場合は開始時刻と終了時刻が必要です。"
            );
        }
    }

    private int elapsedMinutes(LocalTime start, LocalTime end) {
        LocalDateTime startDateTime = LocalDateTime.of(
                java.time.LocalDate.of(2000, 1, 1),
                start
        );
        LocalDateTime endDateTime = LocalDateTime.of(
                java.time.LocalDate.of(2000, 1, 1),
                end
        );
        if (end.isBefore(start)) {
            endDateTime = endDateTime.plusDays(1);
        }
        return Math.toIntExact(Duration.between(startDateTime, endDateTime).toMinutes());
    }

    private int calculateNightMinutes(LocalTime start, int elapsedMinutes) {
        int startMinutes = start.getHour() * 60 + start.getMinute();
        int total = 0;
        for (int offset = 0; offset < elapsedMinutes; offset++) {
            int minuteOfDay = (startMinutes + offset) % MINUTES_PER_DAY;
            if (minuteOfDay >= NIGHT_START_MINUTES
                    || minuteOfDay < NIGHT_END_MINUTES) {
                total++;
            }
        }
        return total;
    }

    private BigDecimal toHours(int minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
