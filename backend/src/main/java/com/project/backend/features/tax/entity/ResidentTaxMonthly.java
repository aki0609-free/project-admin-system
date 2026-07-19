package com.project.backend.features.tax.entity;

import com.project.backend.app.base.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "resident_tax_monthly",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_resident_tax_employee_year_month",
                        columnNames = {"employee_id", "fiscal_year", "month"}
                )
        }
)
@Getter
@Setter
public class ResidentTaxMonthly extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    /**
     * 1〜12
     */
    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "tax_amount", nullable = false)
    private Integer taxAmount;
}