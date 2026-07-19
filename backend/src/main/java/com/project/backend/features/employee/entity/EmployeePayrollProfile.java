package com.project.backend.features.employee.entity;

import java.math.BigDecimal;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.employee.enums.TaxCategory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee_payroll_profile")
@Getter
@Setter
public class EmployeePayrollProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_category", nullable = false, length = 30)
    private TaxCategory taxCategory = TaxCategory.KOU;

    @Column(name = "tax_dependent_count", nullable = false)
    private int taxDependentCount = 0;

    @Column(name = "dependent_flag", nullable = false)
    private boolean dependentFlag = false;

    @Column(name = "dependent_of_other_flag", nullable = false)
    private boolean dependentOfOtherFlag = false;

    @Column(name = "paid_leave_remaining_days", precision = 5, scale = 2)
    private BigDecimal paidLeaveRemainingDays = BigDecimal.ZERO;

    @Column(name = "income_tax_calc_flag", nullable = false)
    private boolean incomeTaxCalcFlag = true;

    @Column(name = "resident_tax_calc_flag", nullable = false)
    private boolean residentTaxCalcFlag = true;

    @Column(name = "resident_tax_monthly", precision = 12, scale = 2)
    private BigDecimal residentTaxMonthly = BigDecimal.ZERO;

    @Column(name = "employment_insurance_flag", nullable = false)
    private boolean employmentInsuranceFlag = true;

    @Column(name = "social_insurance_flag", nullable = false)
    private boolean socialInsuranceFlag = true;

    @Column(name = "health_insurance_flag", nullable = false)
    private boolean healthInsuranceFlag = true;

    @Column(name = "pension_insurance_flag", nullable = false)
    private boolean pensionInsuranceFlag = true;

    @Column(name = "care_insurance_flag", nullable = false)
    private boolean careInsuranceFlag = false;

    @Column(name = "daily_pay_flag", nullable = false)
    private boolean dailyPayFlag = false;

    @Column(name = "commute_allowance_monthly", precision = 12, scale = 2)
    private BigDecimal commuteAllowanceMonthly = BigDecimal.ZERO;
}