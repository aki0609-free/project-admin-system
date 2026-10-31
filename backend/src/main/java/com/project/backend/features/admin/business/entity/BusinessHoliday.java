package com.project.backend.features.admin.business.entity;

import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "business_holiday")
@Getter
@Setter
public class BusinessHoliday extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "holiday_name", nullable = false, length = 100)
    private String holidayName;

    @Column(name = "holiday_type", nullable = false, length = 30)
    private String holidayType = "OBON";

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}
