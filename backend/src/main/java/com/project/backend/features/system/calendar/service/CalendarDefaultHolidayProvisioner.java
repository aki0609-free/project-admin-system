package com.project.backend.features.system.calendar.service;

import java.time.LocalDate;
import java.time.DayOfWeek;

import org.springframework.stereotype.Service;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.calendar.entity.SystemCalendarHoliday;
import com.project.backend.features.system.calendar.entity.SystemCalendarWeekdayRule;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.repository.SystemCalendarHolidayRepository;
import com.project.backend.features.system.calendar.repository.SystemCalendarWeekdayRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CalendarDefaultHolidayProvisioner {
    private final SystemCalendarHolidayRepository repository;
    private final SystemCalendarWeekdayRuleRepository weekdayRuleRepository;

    public void ensureCompanyDefaults(int year) {
        String tenantId = TenantContext.getTenantId();
        ensureCompanyWeekdayDefault(tenantId);
        for (int day = 13; day <= 15; day++) {
            LocalDate date = LocalDate.of(year, 8, day);
            if (repository
                    .findByTenantIdAndCalendarTypeAndHolidayDateAndDeletedAtIsNull(
                            tenantId, CalendarType.COMPANY, date
                    )
                    .isPresent()) {
                continue;
            }
            SystemCalendarHoliday holiday = new SystemCalendarHoliday();
            holiday.setTenantId(tenantId);
            holiday.setCalendarType(CalendarType.COMPANY);
            holiday.setHolidayDate(date);
            holiday.setHolidayName("お盆休み");
            holiday.setActiveFlag(true);
            repository.save(holiday);
        }
    }

    public void ensureCompanyPolicyDefaults() {
        ensureCompanyWeekdayDefault(TenantContext.getTenantId());
    }

    private void ensureCompanyWeekdayDefault(String tenantId) {
        if (weekdayRuleRepository.existsByTenantIdAndCalendarTypeAndDeletedAtIsNull(
                tenantId, CalendarType.COMPANY
        )) {
            return;
        }
        SystemCalendarWeekdayRule rule = new SystemCalendarWeekdayRule();
        rule.setTenantId(tenantId);
        rule.setCalendarType(CalendarType.COMPANY);
        rule.setDayOfWeek(DayOfWeek.SUNDAY);
        rule.setClosedFlag(true);
        weekdayRuleRepository.save(rule);
    }
}
