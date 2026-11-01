package com.project.backend.features.dailyreport.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.project.backend.features.dailyreport.dto.DailyReportDateSuggestionResponse;
import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.system.calendar.enums.CalendarRollDirection;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.service.CalendarAdjustmentService;
import com.project.backend.features.system.calendar.service.CalendarDayPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyReportDateSuggestionService {
    private static final CalendarDayPolicy WORK_DATE_POLICY = new CalendarDayPolicy(
            Set.of(CalendarType.COMPANY), Set.of(DayOfWeek.SUNDAY)
    );
    private static final CalendarDayPolicy DAILY_WEEKLY_PAYMENT_POLICY = new CalendarDayPolicy(
            Set.of(CalendarType.COMPANY, CalendarType.FINANCIAL_INSTITUTION),
            Set.of(DayOfWeek.SUNDAY)
    );
    private static final CalendarDayPolicy MONTHLY_PAYMENT_POLICY = new CalendarDayPolicy(
            Set.of(CalendarType.COMPANY, CalendarType.FINANCIAL_INSTITUTION),
            Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    );

    private final CalendarAdjustmentService calendar;

    public DailyReportDateSuggestionResponse resolve(LocalDate businessDate, LocalDate workDate,
            PaymentCycle paymentCycle) {
        LocalDate resolvedWorkDate = workDate != null
                ? workDate
                : calendar.previousOpenDayBefore(businessDate, WORK_DATE_POLICY);
        LocalDate paymentDate = switch (paymentCycle) {
            case DAILY -> calendar.adjust(
                    resolvedWorkDate.plusDays(1), CalendarRollDirection.FORWARD,
                    DAILY_WEEKLY_PAYMENT_POLICY
            );
            case WEEKLY -> calendar.adjust(
                    resolvedWorkDate.plusDays(daysToFollowingMonday(resolvedWorkDate)),
                    CalendarRollDirection.FORWARD, DAILY_WEEKLY_PAYMENT_POLICY
            );
            case MONTHLY -> calendar.adjust(
                    resolvedWorkDate.plusMonths(1).withDayOfMonth(15),
                    CalendarRollDirection.BACKWARD, MONTHLY_PAYMENT_POLICY
            );
        };
        return new DailyReportDateSuggestionResponse(
                resolvedWorkDate, paymentDate);
    }

    private long daysToFollowingMonday(LocalDate workDate) {
        int weekday = workDate.getDayOfWeek().getValue();
        return workDate.getDayOfWeek() == DayOfWeek.SUNDAY ? 1 : 8L - weekday;
    }
}
