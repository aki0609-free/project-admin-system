package com.project.backend.features.system.calendar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.calendar.entity.SystemCalendarReference;
import com.project.backend.features.system.calendar.enums.CalendarType;

public interface SystemCalendarReferenceRepository
        extends JpaRepository<SystemCalendarReference, Long> {

    List<SystemCalendarReference>
            findByTenantIdAndCalendarTypeAndDeletedAtIsNull(
                    String tenantId, CalendarType calendarType
            );

    List<SystemCalendarReference>
            findByTenantIdAndCalendarTypeAndActiveFlagTrueAndDeletedAtIsNull(
                    String tenantId, CalendarType calendarType
            );
}
