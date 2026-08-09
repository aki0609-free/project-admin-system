package com.project.backend.features.master.payrollitem.balance;

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
@Table(name = "employee_payroll_item_enrollment")
@Getter
@Setter
public class EmployeePayrollItemEnrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "balance_policy_id", nullable = false)
    private Long balancePolicyId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "settings_json", columnDefinition = "json")
    private String settingsJson;
}
