package com.project.backend.features.operation.preparation.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "daily_preparation_assignments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "preparation_id", "employee_id"})
        }
)
@Getter
@Setter
public class DailyPreparationAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "preparation_id", nullable = false)
    private Long preparationId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_code", length = 100)
    private String employeeCode;

    @Column(name = "employee_name", length = 200)
    private String employeeName;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_site_id")
    private Long customerSiteId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "site_name", length = 255)
    private String siteName;

    @Column(name = "work_description", length = 1000)
    private String workDescription;
}