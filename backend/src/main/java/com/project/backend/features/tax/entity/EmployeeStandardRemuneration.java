package com.project.backend.features.tax.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "employee_standard_remuneration",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_employee_standard_remuneration_from",
                columnNames = {"tenant_id", "employee_id", "effective_from"}
        )
)
@Getter
@Setter
public class EmployeeStandardRemuneration extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "health_standard_remuneration", nullable = false, precision = 15, scale = 2)
    private BigDecimal healthStandardRemuneration;

    @Column(name = "pension_standard_remuneration", nullable = false, precision = 15, scale = 2)
    private BigDecimal pensionStandardRemuneration;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "note", length = 1000)
    private String note;
}
