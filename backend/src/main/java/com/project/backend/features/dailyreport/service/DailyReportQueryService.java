package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportDetailResponse;
import com.project.backend.features.dailyreport.dto.DailyReportResponse;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.mapper.DailyReportMapper;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.EmployeePayrollProfile;
import com.project.backend.features.employee.repository.EmployeePayrollProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportQueryService {

    private final DailyReportRepository repository;
    private final DailyReportMapper mapper;

    private final DailyReportAllowanceQueryService allowanceQueryService;
    private final DailyReportDeductionQueryService deductionQueryService;

    private final EmployeePayrollProfileRepository payrollProfileRepository;

    public List<DailyReportResponse> findAll(
            LocalDate from,
            LocalDate to,
            Long employeeId
    ) {
        List<DailyReport> reports;

        if (employeeId != null) {
            reports = repository
                    .findByEmployeeIdAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                            employeeId
                    );
        } else if (from != null && to != null) {
            reports = repository
                    .findByWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                            from,
                            to
                    );
        } else {
            reports = repository
                    .findAllByDeletedAtIsNullOrderByWorkDateDescIdDesc();
        }

        Map<Long, BigDecimal> paidLeaveRemainingDaysMap = reports.stream()
                .filter(report -> report.getEmployee() != null)
                .map(report -> report.getEmployee().getId())
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        this::findPaidLeaveRemainingDays
                ));

        return reports.stream()
                .map(report -> {
                    DailyReportResponse response =
                            mapper.toResponse(report);

                    Long currentEmployeeId =
                            report.getEmployee() != null
                                    ? report.getEmployee().getId()
                                    : null;

                    BigDecimal paidLeaveRemainingDays =
                            currentEmployeeId != null
                                    ? paidLeaveRemainingDaysMap.getOrDefault(
                                            currentEmployeeId,
                                            BigDecimal.ZERO
                                    )
                                    : BigDecimal.ZERO;

                    BigDecimal paidLeaveDays =
                            nvl(response.paidLeaveDays());

                    BigDecimal paidLeaveRemainingAfterUsedDays =
                            paidLeaveRemainingDays.subtract(
                                    paidLeaveDays
                            );

                    return DailyReportResponse.builder()
                            .id(response.id())

                            .employeeId(response.employeeId())
                            .employeeCode(response.employeeCode())
                            .employeeName(response.employeeName())

                            .workDate(response.workDate())
                            .paymentDate(response.paymentDate())

                            .customerId(response.customerId())
                            .customerSiteId(response.customerSiteId())

                            .customerName(response.customerName())
                            .siteName(response.siteName())

                            .billingRateId(
                                    response.billingRateId()
                            )

                            .jobCode(
                                    response.jobCode()
                            )
                            .jobName(
                                    response.jobName()
                            )

                            .siteRoleCode(
                                    response.siteRoleCode()
                            )
                            .siteRoleName(
                                    response.siteRoleName()
                            )

                            .billingUnit(
                                    response.billingUnit()
                            )

                            .billingBaseUnitPrice(
                                    response.billingBaseUnitPrice()
                            )
                            .billingOvertimeUnitPrice(
                                    response.billingOvertimeUnitPrice()
                            )
                            .billingNightUnitPrice(
                                    response.billingNightUnitPrice()
                            )
                            .billingHolidayUnitPrice(
                                    response.billingHolidayUnitPrice()
                            )
                            .billingCommuteUnitPrice(
                                    response.billingCommuteUnitPrice()
                            )

                            .workDescription(
                                    response.workDescription()
                            )

                            .startTime(response.startTime())
                            .endTime(response.endTime())
                            .breakMinutes(response.breakMinutes())

                            .workHours(
                                    response.workHours()
                            )
                            .overtimeHours(
                                    response.overtimeHours()
                            )
                            .nightWorkHours(
                                    response.nightWorkHours()
                            )
                            .holidayWorkHours(
                                    response.holidayWorkHours()
                            )

                            .allowanceAmount(
                                    response.allowanceAmount()
                            )
                            .deductionAmount(
                                    response.deductionAmount()
                            )

                            .loanRepaymentAmount(
                                    response.loanRepaymentAmount()
                            )
                            .savingAmount(
                                    response.savingAmount()
                            )

                            .estimatedGrossPayAmount(
                                    response.estimatedGrossPayAmount()
                            )
                            .estimatedNetPayAmount(
                                    response.estimatedNetPayAmount()
                            )

                            .vehicleUsedFlag(
                                    response.vehicleUsedFlag()
                            )
                            .mileage(
                                    response.mileage()
                            )

                            .paidLeaveDays(
                                    paidLeaveDays
                            )
                            .paidLeaveRemainingDays(
                                    paidLeaveRemainingDays
                            )
                            .paidLeaveRemainingAfterUsedDays(
                                    paidLeaveRemainingAfterUsedDays
                            )

                            .approvalStatus(
                                    response.approvalStatus()
                            )
                            .approvalComment(
                                    response.approvalComment()
                            )
                            .build();
                })
                .toList();
    }

    public DailyReportDetailResponse findDetail(
            Long id
    ) {
        DailyReport entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "日報が見つかりません。 id=" + id
                                )
                        );

        Long employeeId =
                entity.getEmployee() != null
                        ? entity.getEmployee().getId()
                        : null;

        BigDecimal paidLeaveRemainingDays =
                employeeId != null
                        ? findPaidLeaveRemainingDays(employeeId)
                        : BigDecimal.ZERO;

        BigDecimal paidLeaveDays =
                nvl(entity.getPaidLeaveDays());

        BigDecimal paidLeaveRemainingAfterUsedDays =
                paidLeaveRemainingDays.subtract(
                        paidLeaveDays
                );

        return DailyReportDetailResponse.builder()
                .id(entity.getId())

                .employeeId(
                        entity.getEmployee().getId()
                )
                .employeeCode(
                        entity.getEmployee().getEmployeeCode()
                )
                .employeeName(
                        entity.getEmployee().getEmployeeName()
                )

                .workDate(
                        entity.getWorkDate()
                )
                .paymentDate(
                        entity.getPaymentDate()
                )

                .customerId(
                        entity.getCustomerId()
                )
                .customerSiteId(
                        entity.getCustomerSiteId()
                )

                .customerName(
                        entity.getCustomerName()
                )
                .siteName(
                        entity.getSiteName()
                )

                .billingRateId(
                        entity.getBillingRateId()
                )

                .jobCode(
                        entity.getJobCode()
                )
                .jobName(
                        entity.getJobName()
                )

                .siteRoleCode(
                        entity.getSiteRoleCode()
                )
                .siteRoleName(
                        entity.getSiteRoleName()
                )

                .billingUnit(
                        entity.getBillingUnit()
                )

                .billingBaseUnitPrice(
                        entity.getBillingBaseUnitPrice()
                )
                .billingOvertimeUnitPrice(
                        entity.getBillingOvertimeUnitPrice()
                )
                .billingNightUnitPrice(
                        entity.getBillingNightUnitPrice()
                )
                .billingHolidayUnitPrice(
                        entity.getBillingHolidayUnitPrice()
                )
                .billingCommuteUnitPrice(
                        entity.getBillingCommuteUnitPrice()
                )

                .workDescription(
                        entity.getWorkDescription()
                )

                .startTime(
                        entity.getStartTime()
                )
                .endTime(
                        entity.getEndTime()
                )

                .breakMinutes(
                        entity.getBreakMinutes()
                )

                .workHours(
                        entity.getWorkHours()
                )
                .overtimeHours(
                        entity.getOvertimeHours()
                )
                .nightWorkHours(
                        entity.getNightWorkHours()
                )
                .holidayWorkHours(
                        entity.getHolidayWorkHours()
                )

                .allowanceAmount(
                        entity.getAllowanceAmount()
                )
                .deductionAmount(
                        entity.getDeductionAmount()
                )

                .loanRepaymentAmount(
                        entity.getLoanRepaymentAmount()
                )
                .savingAmount(
                        entity.getSavingAmount()
                )

                .estimatedGrossPayAmount(
                        entity.getEstimatedGrossPayAmount()
                )
                .estimatedNetPayAmount(
                        entity.getEstimatedNetPayAmount()
                )

                .vehicleUsedFlag(
                        entity.isVehicleUsedFlag()
                )
                .mileage(
                        entity.getMileage()
                )

                .paidLeaveDays(
                        paidLeaveDays
                )
                .paidLeaveRemainingDays(
                        paidLeaveRemainingDays
                )
                .paidLeaveRemainingAfterUsedDays(
                        paidLeaveRemainingAfterUsedDays
                )

                .approvalStatus(
                        entity.getApprovalStatus()
                )
                .approvalComment(
                        entity.getApprovalComment()
                )

                .allowances(
                        allowanceQueryService
                                .findByDailyReportId(id)
                )
                .deductions(
                        deductionQueryService
                                .findByDailyReportId(id)
                )
                .build();
    }

    @SuppressWarnings("null")
    private BigDecimal findPaidLeaveRemainingDays(
            Long employeeId
    ) {
        return payrollProfileRepository
                .findByEmployeeIdAndDeletedAtIsNull(
                        employeeId
                )
                .map(
                        EmployeePayrollProfile
                                ::getPaidLeaveRemainingDays
                )
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal nvl(
            BigDecimal value
    ) {
        return value != null
                ? value
                : BigDecimal.ZERO;
    }
}