package com.project.backend.features.system.calendar.entity;

import java.time.DayOfWeek;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.calendar.enums.CalendarType;

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
@Table(name = "system_calendar_weekday_rule", uniqueConstraints = @UniqueConstraint(
        columnNames = {"tenant_id", "calendar_type", "day_of_week"}
))
@Getter
@Setter
public class SystemCalendarWeekdayRule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_type", nullable = false, length = 40)
    private CalendarType calendarType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "closed_flag", nullable = false)
    private boolean closedFlag = true;
}
