package com.project.backend.features.dailyreport.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.project.backend.features.admin.business.service.BusinessCalendarService;
import com.project.backend.features.dailyreport.dto.DailyReportDateSuggestionResponse;
import com.project.backend.features.employee.enums.PaymentCycle;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyReportDateSuggestionService {
    private final BusinessCalendarService calendar;

    public DailyReportDateSuggestionResponse resolve(LocalDate businessDate, LocalDate workDate,
            PaymentCycle paymentCycle) {
        LocalDate resolvedWorkDate = workDate != null ? workDate : calendar.previousBusinessDay(businessDate);
        LocalDate paymentDate = switch (paymentCycle) {
            case DAILY -> calendar.nextBusinessDay(resolvedWorkDate.plusDays(1));
            case WEEKLY -> calendar.nextBusinessDay(
                    resolvedWorkDate.plusDays(daysToFollowingMonday(resolvedWorkDate))
            );
            case MONTHLY -> calendar.previousMonthlyPaymentBusinessDay(
                    resolvedWorkDate.plusMonths(1).withDayOfMonth(15)
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
