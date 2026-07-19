package com.project.backend.features.employee.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.enums.SalaryType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee_contract")
@Getter
@Setter
public class EmployeeContract extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "renewal_flag", nullable = false)
    private boolean renewalFlag = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", nullable = false, length = 30)
    private SalaryType salaryType = SalaryType.MONTHLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_cycle", nullable = false, length = 30)
    private PaymentCycle paymentCycle = PaymentCycle.MONTHLY;

    @Column(name = "monthly_salary", precision = 12, scale = 2)
    private BigDecimal monthlySalary = BigDecimal.ZERO;

    @Column(name = "weekly_wage", precision = 12, scale = 2)
    private BigDecimal weeklyWage = BigDecimal.ZERO;

    @Column(name = "daily_wage", precision = 12, scale = 2)
    private BigDecimal dailyWage = BigDecimal.ZERO;

    @Column(name = "hourly_wage", precision = 12, scale = 2)
    private BigDecimal hourlyWage = BigDecimal.ZERO;

    @Column(name = "standard_working_hours", precision = 5, scale = 2)
    private BigDecimal standardWorkingHours = BigDecimal.ZERO;

    @Column(name = "note", length = 1000)
    private String note;
}