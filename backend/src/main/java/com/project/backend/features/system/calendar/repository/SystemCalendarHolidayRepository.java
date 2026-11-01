package com.project.backend.features.system.calendar.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.calendar.entity.SystemCalendarHoliday;
import com.project.backend.features.system.calendar.enums.CalendarType;

public interface SystemCalendarHolidayRepository
        extends JpaRepository<SystemCalendarHoliday, Long> {

    List<SystemCalendarHoliday>
            findByTenantIdAndCalendarTypeAndHolidayDateBetweenAndDeletedAtIsNullOrderByHolidayDateAsc(
                    String tenantId,
                    CalendarType calendarType,
                    LocalDate from,
                    LocalDate to
            );

    Optional<SystemCalendarHoliday>
            findByIdAndTenantIdAndDeletedAtIsNull(Long id, String tenantId);

    Optional<SystemCalendarHoliday>
            findByTenantIdAndCalendarTypeAndHolidayDateAndDeletedAtIsNull(
                    String tenantId,
                    CalendarType calendarType,
                    LocalDate holidayDate
            );

    Optional<SystemCalendarHoliday>
            findByTenantIdAndCalendarTypeAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
                    String tenantId,
                    CalendarType calendarType,
                    LocalDate holidayDate
            );

    boolean existsByTenantIdAndCalendarTypeInAndHolidayDateAndActiveFlagTrueAndDeletedAtIsNull(
            String tenantId,
            Collection<CalendarType> calendarTypes,
            LocalDate holidayDate
    );
}
