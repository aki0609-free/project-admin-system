package com.project.backend.features.system.calendar.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.calendar.enums.CalendarRollDirection;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarAdjustmentService {
    private static final int MAX_SEARCH_DAYS = 366;

    private final CalendarQueryService queryService;

    public LocalDate previousOpenDayBefore(LocalDate date, CalendarDayPolicy policy) {
        return adjust(date.minusDays(1), CalendarRollDirection.BACKWARD, policy);
    }

    public LocalDate adjust(
            LocalDate date,
            CalendarRollDirection direction,
            CalendarDayPolicy policy
    ) {
        LocalDate candidate = date;
        for (int i = 0; i < MAX_SEARCH_DAYS; i++) {
            if (!queryService.isClosed(candidate, policy)) return candidate;
            candidate = direction == CalendarRollDirection.FORWARD
                    ? candidate.plusDays(1)
                    : candidate.minusDays(1);
        }
        throw new IllegalStateException("営業日を特定できませんでした。");
    }
}
