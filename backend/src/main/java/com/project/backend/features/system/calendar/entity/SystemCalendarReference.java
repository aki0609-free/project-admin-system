package com.project.backend.features.system.calendar.entity;

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
@Table(name = "system_calendar_reference", uniqueConstraints = @UniqueConstraint(
        columnNames = {"tenant_id", "calendar_type", "source_calendar_type"}
))
@Getter
@Setter
public class SystemCalendarReference extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_type", nullable = false, length = 40)
    private CalendarType calendarType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_calendar_type", nullable = false, length = 40)
    private CalendarType sourceCalendarType;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}
