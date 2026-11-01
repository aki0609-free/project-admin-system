package com.project.backend.features.system.calendar.service;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.repository.SystemCalendarHolidayRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarQueryService {
    private final SystemCalendarHolidayRepository repository;
    private final CalendarDefaultHolidayProvisioner defaultHolidayProvisioner;

    public boolean isClosed(LocalDate date, CalendarDayPolicy policy) {
        if (date == null) return false;
        if (policy.closedWeekdays().contains(date.getDayOfWeek())) return true;
        Set<CalendarType> types = policy.calendarTypes();
        if (types.isEmpty()) return false;
        if (types.contains(CalendarType.COMPANY)) {
            defaultHolidayProvisioner.ensureCompanyDefaults(date.getYear());
        }
        return repository
                .existsByTenantIdAndCalendarTypeInAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                        TenantContext.getTenantId(), types, date
                );
    }
}
