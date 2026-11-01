package com.project.backend.features.system.calendar.entity;

import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.enums.CalendarDayType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "system_calendar_holiday",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"tenant_id", "calendar_type", "holiday_date"}
        )
)
@Getter
@Setter
public class SystemCalendarHoliday extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_type", nullable = false, length = 40)
    private CalendarType calendarType;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "holiday_name", nullable = false, length = 100)
    private String holidayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false, length = 20)
    private CalendarDayType dayType = CalendarDayType.CLOSED;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}
