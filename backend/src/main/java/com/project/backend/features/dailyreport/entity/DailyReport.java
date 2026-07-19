package com.project.backend.features.dailyreport.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.customer.enums.CustomerBillingUnit;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.enums.ApprovalStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "daily_report")
@Getter
@Setter
public class DailyReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_site_id")
    private Long customerSiteId;

    /**
     * 日報入力時に適用された請求単価マスタID。
     * 過去データの追跡用として保持する。
     */
    @Column(name = "billing_rate_id")
    private Long billingRateId;

    /**
     * 日報入力時点の職種スナップショット。
     */
    @Column(name = "job_code", length = 100)
    private String jobCode;

    @Column(name = "job_name", length = 200)
    private String jobName;

    /**
     * 現場での役職スナップショット。
     * 役職なしの場合は GENERAL / 一般を想定。
     */
    @Column(name = "site_role_code", length = 100)
    private String siteRoleCode;

    @Column(name = "site_role_name", length = 200)
    private String siteRoleName;

    /**
     * 請求単価の単位。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_unit", length = 30)
    private CustomerBillingUnit billingUnit;

    /**
     * 日報入力時点の請求単価スナップショット。
     * 月次請求では単価マスタを再参照せず、この値を使用する。
     */
    @Column(
            name = "billing_base_unit_price",
            precision = 15,
            scale = 2,
            nullable = false
    )
    private BigDecimal billingBaseUnitPrice = BigDecimal.ZERO;

    @Column(
            name = "billing_overtime_unit_price",
            precision = 15,
            scale = 2,
            nullable = false
    )
    private BigDecimal billingOvertimeUnitPrice = BigDecimal.ZERO;

    @Column(
            name = "billing_night_unit_price",
            precision = 15,
            scale = 2,
            nullable = false
    )
    private BigDecimal billingNightUnitPrice = BigDecimal.ZERO;

    /**
     * 休日労働時間に適用する請求単価スナップショット。
     */
    @Column(
            name = "billing_holiday_unit_price",
            precision = 15,
            scale = 2,
            nullable = false
    )
    private BigDecimal billingHolidayUnitPrice = BigDecimal.ZERO;

    /**
     * 走行距離1kmあたりの請求単価スナップショット。
     */
    @Column(
            name = "billing_commute_unit_price",
            precision = 15,
            scale = 2,
            nullable = false
    )
    private BigDecimal billingCommuteUnitPrice = BigDecimal.ZERO;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "site_name", length = 255)
    private String siteName;

    @Column(name = "work_description", length = 1000)
    private String workDescription;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "break_minutes")
    private Integer breakMinutes = 0;

    /**
     * 通常労働時間。
     *
     * 休日労働時間は含めず、
     * holidayWorkHoursへ分離して保持する。
     */
    @Column(
            name = "work_hours",
            precision = 5,
            scale = 2
    )
    private BigDecimal workHours = BigDecimal.ZERO;

    /**
     * 早出・残業時間。
     *
     * 通常日・休日のどちらの残業かは、
     * 将来的に区分追加が必要になった場合に拡張する。
     */
    @Column(
            name = "overtime_hours",
            precision = 5,
            scale = 2
    )
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    /**
     * 深夜時間。
     *
     * 通常時間・休日時間・残業時間と重複して保持できる。
     */
    @Column(
            name = "night_work_hours",
            precision = 5,
            scale = 2
    )
    private BigDecimal nightWorkHours = BigDecimal.ZERO;

    /**
     * 休日労働時間。
     *
     * 通常労働時間 workHours とは分離して保持する。
     * 休日に8時間勤務した場合は、
     * workHours=0、holidayWorkHours=8を基本とする。
     */
    @Column(
            name = "holiday_work_hours",
            precision = 5,
            scale = 2
    )
    private BigDecimal holidayWorkHours = BigDecimal.ZERO;

    @Column(
            name = "allowance_amount",
            precision = 12,
            scale = 2
    )
    private BigDecimal allowanceAmount = BigDecimal.ZERO;

    @Column(
            name = "deduction_amount",
            precision = 12,
            scale = 2
    )
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    @Column(
            name = "loan_repayment_amount",
            precision = 12,
            scale = 2
    )
    private BigDecimal loanRepaymentAmount = BigDecimal.ZERO;

    @Column(
            name = "saving_amount",
            precision = 12,
            scale = 2
    )
    private BigDecimal savingAmount = BigDecimal.ZERO;

    @Column(
            name = "estimated_gross_pay_amount",
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal estimatedGrossPayAmount = BigDecimal.ZERO;

    @Column(
            name = "estimated_net_pay_amount",
            precision = 12,
            scale = 2,
            nullable = false
    )
    private BigDecimal estimatedNetPayAmount = BigDecimal.ZERO;

    @Column(
            name = "vehicle_used_flag",
            nullable = false
    )
    private boolean vehicleUsedFlag = false;

    @Column(
            name = "mileage",
            precision = 8,
            scale = 2
    )
    private BigDecimal mileage = BigDecimal.ZERO;

    @Column(
            name = "paid_leave_days",
            precision = 5,
            scale = 2
    )
    private BigDecimal paidLeaveDays = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "approval_status",
            nullable = false,
            length = 30
    )
    private ApprovalStatus approvalStatus =
            ApprovalStatus.PENDING;

    @Column(
            name = "approval_comment",
            length = 1000
    )
    private String approvalComment;
}