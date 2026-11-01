package com.project.backend.features.system.calendar.repository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.calendar.entity.SystemCalendarWeekdayRule;
import com.project.backend.features.system.calendar.enums.CalendarType;

public interface SystemCalendarWeekdayRuleRepository
        extends JpaRepository<SystemCalendarWeekdayRule, Long> {

    List<SystemCalendarWeekdayRule>
            findByTenantIdAndCalendarTypeAndDeletedAtIsNull(
                    String tenantId, CalendarType calendarType
            );

    boolean existsByTenantIdAndCalendarTypeAndDayOfWeekAndClosedFlagTrueAndDeletedAtIsNull(
            String tenantId, CalendarType calendarType, DayOfWeek dayOfWeek
    );

    boolean existsByTenantIdAndCalendarTypeAndDeletedAtIsNull(
            String tenantId, CalendarType calendarType
    );
}
