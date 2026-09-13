package com.project.backend.features.operation.daily.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.enums.ApprovalStatus;
import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.operation.daily.dto.DailyPaymentDenominationResponse;
import com.project.backend.features.operation.daily.dto.DailyPaymentPrintDetailResponse;
import com.project.backend.features.operation.daily.dto.DailyPaymentPrintSummaryResponse;
import com.project.backend.features.operation.daily.dto.DailyPaymentResponse;
import com.project.backend.features.operation.daily.entity.DailyPayment;
import com.project.backend.features.operation.daily.enums.DailyPaymentStatus;
import com.project.backend.features.operation.daily.mapper.DailyPaymentMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPaymentService {

    private final DailyReportRepository dailyReportRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final DailyPaymentMapper mapper;

    @Transactional(readOnly = true)
    public List<DailyPaymentResponse> findByPaymentDate(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new RuntimeException("paymentDate は必須です。");
        }

        List<DailyReport> reports = dailyReportRepository
                .findByPaymentDateAndApprovalStatusAndDeletedAtIsNullOrderByEmployeeEmployeeCodeAscWorkDateDescIdDesc(
                        paymentDate,
                        ApprovalStatus.APPROVED
                );

        Map<Long, List<DailyReport>> reportsByEmployee = new LinkedHashMap<>();
        for (DailyReport report : reports) {
            reportsByEmployee
                    .computeIfAbsent(
                            report.getEmployee().getId(),
                            ignored -> new ArrayList<>()
                    )
                    .add(report);
        }

        Set<Long> dailyPaymentEmployeeIds = employeeContractRepository
                .findByEmployeeIdInAndDeletedAtIsNull(reportsByEmployee.keySet())
                .stream()
                .filter(contract -> contract.getPaymentCycle() == PaymentCycle.DAILY)
                .map(contract -> contract.getEmployee().getId())
                .collect(Collectors.toSet());

        return reportsByEmployee.entrySet()
                .stream()
                .filter(entry -> dailyPaymentEmployeeIds.contains(entry.getKey()))
                .map(entry -> createGeneratedPayment(paymentDate, entry.getValue()))
                .sorted((left, right) -> nvlText(left.getEmployeeCode())
                        .compareTo(nvlText(right.getEmployeeCode())))
                .map(mapper::toResponse)
                .toList();
    }

    @SuppressWarnings("null")
    @Transactional(readOnly = true)
    public DailyPaymentPrintSummaryResponse getPrintSummary(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new RuntimeException("paymentDate は必須です。");
        }

        List<DailyPaymentResponse> payments = findByPaymentDate(paymentDate);

        List<DailyPaymentPrintDetailResponse> details = payments.stream()
                .map(payment -> {
                    BigDecimal plannedAmount = nvl(payment.plannedAmount());
                    BigDecimal actualAmount = nvl(payment.actualAmount());

                    return DailyPaymentPrintDetailResponse.builder()
                            .employeeId(payment.employeeId())
                            .employeeCode(payment.employeeCode())
                            .employeeName(payment.employeeName())
                            .plannedAmount(plannedAmount)
                            .actualAmount(actualAmount)
                            .note(payment.note())
                            .denomination(calculateDenomination(actualAmount))
                            .build();
                })
                .toList();

        BigDecimal totalPlannedAmount = details.stream()
                .map(DailyPaymentPrintDetailResponse::plannedAmount)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalActualAmount = details.stream()
                .map(DailyPaymentPrintDetailResponse::actualAmount)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DailyPaymentPrintSummaryResponse.builder()
                .paymentDate(paymentDate)
                .employeeCount(details.size())
                .totalPlannedAmount(totalPlannedAmount)
                .totalActualAmount(totalActualAmount)
                .totalDenomination(sumDenominations(details))
                .details(details)
                .build();
    }

    private DailyPayment createGeneratedPayment(
            LocalDate paymentDate,
            List<DailyReport> reports
    ) {
        if (reports == null || reports.isEmpty()) {
            throw new IllegalArgumentException("日次支払の元となる日報が必要です。");
        }
        DailyReport representative = reports.getFirst();
        DailyPayment entity = new DailyPayment();

        entity.setId(null);
        entity.setPaymentDate(paymentDate);

        entity.setEmployeeId(representative.getEmployee().getId());
        entity.setEmployeeCode(representative.getEmployee().getEmployeeCode());
        entity.setEmployeeName(representative.getEmployee().getEmployeeName());

        BigDecimal plannedAmount = reports.stream()
                .map(this::calculatePlannedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        entity.setPlannedAmount(plannedAmount);
        entity.setActualAmount(plannedAmount);
        entity.setStatus(DailyPaymentStatus.PENDING);
        entity.setNote(null);

        return entity;
    }

    private BigDecimal calculatePlannedAmount(DailyReport report) {
        return nvl(report.getEstimatedNetPayAmount());
    }

    private DailyPaymentDenominationResponse calculateDenomination(BigDecimal amount) {
        int remaining = amount != null ? amount.intValue() : 0;

        if (remaining < 0) {
            remaining = 0;
        }

        int yen10000 = remaining / 10000;
        remaining %= 10000;

        int yen5000 = remaining / 5000;
        remaining %= 5000;

        int yen1000 = remaining / 1000;
        remaining %= 1000;

        int yen500 = remaining / 500;
        remaining %= 500;

        int yen100 = remaining / 100;
        remaining %= 100;

        int yen50 = remaining / 50;
        remaining %= 50;

        int yen10 = remaining / 10;
        remaining %= 10;

        int yen5 = remaining / 5;
        remaining %= 5;

        int yen1 = remaining;

        return DailyPaymentDenominationResponse.builder()
                .yen10000(yen10000)
                .yen5000(yen5000)
                .yen1000(yen1000)
                .yen500(yen500)
                .yen100(yen100)
                .yen50(yen50)
                .yen10(yen10)
                .yen5(yen5)
                .yen1(yen1)
                .build();
    }

    private DailyPaymentDenominationResponse sumDenominations(
            List<DailyPaymentPrintDetailResponse> details
    ) {
        return DailyPaymentDenominationResponse.builder()
                .yen10000(sum(details, denomination -> denomination.yen10000()))
                .yen5000(sum(details, denomination -> denomination.yen5000()))
                .yen1000(sum(details, denomination -> denomination.yen1000()))
                .yen500(sum(details, denomination -> denomination.yen500()))
                .yen100(sum(details, denomination -> denomination.yen100()))
                .yen50(sum(details, denomination -> denomination.yen50()))
                .yen10(sum(details, denomination -> denomination.yen10()))
                .yen5(sum(details, denomination -> denomination.yen5()))
                .yen1(sum(details, denomination -> denomination.yen1()))
                .build();
    }

    private int sum(
            List<DailyPaymentPrintDetailResponse> details,
            java.util.function.ToIntFunction<DailyPaymentDenominationResponse> getter
    ) {
        return details.stream()
                .map(DailyPaymentPrintDetailResponse::denomination)
                .mapToInt(getter)
                .sum();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String nvlText(String value) {
        return value != null ? value : "";
    }
}
