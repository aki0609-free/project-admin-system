package com.project.backend.features.operation.daily.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.operation.daily.dto.DailyPaymentBulkSaveItemRequest;
import com.project.backend.features.operation.daily.dto.DailyPaymentBulkSaveRequest;
import com.project.backend.features.operation.daily.dto.DailyPaymentDenominationResponse;
import com.project.backend.features.operation.daily.dto.DailyPaymentPrintDetailResponse;
import com.project.backend.features.operation.daily.dto.DailyPaymentPrintSummaryResponse;
import com.project.backend.features.operation.daily.dto.DailyPaymentResponse;
import com.project.backend.features.operation.daily.entity.DailyPayment;
import com.project.backend.features.operation.daily.enums.DailyPaymentStatus;
import com.project.backend.features.operation.daily.mapper.DailyPaymentMapper;
import com.project.backend.features.operation.daily.repository.DailyPaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyPaymentService {

    private final DailyPaymentRepository dailyPaymentRepository;
    private final DailyReportRepository dailyReportRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final DailyPaymentMapper mapper;

    @Transactional(readOnly = true)
    public List<DailyPaymentResponse> findByPaymentDate(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new RuntimeException("paymentDate は必須です。");
        }

        Map<Long, DailyPayment> paymentMap = new LinkedHashMap<>();

        dailyPaymentRepository
                .findByPaymentDateAndDeletedAtIsNullOrderByEmployeeCodeAscIdAsc(paymentDate)
                .forEach(payment -> paymentMap.put(payment.getEmployeeId(), payment));

        List<DailyReport> reports = dailyReportRepository
                .findByPaymentDateAndDeletedAtIsNullOrderByWorkDateDescIdDesc(paymentDate);

        for (DailyReport report : reports) {
            Long employeeId = report.getEmployee().getId();

            if (paymentMap.containsKey(employeeId)) {
                continue;
            }

            DailyPayment generated = createGeneratedPayment(paymentDate, report);
            paymentMap.put(employeeId, generated);
        }

        return paymentMap.values()
                .stream()
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
                .totalDenomination(calculateDenomination(totalActualAmount))
                .details(details)
                .build();
    }

    public List<DailyPaymentResponse> bulkSave(DailyPaymentBulkSaveRequest request) {
        if (request == null || request.getPaymentDate() == null) {
            throw new RuntimeException("paymentDate は必須です。");
        }

        for (DailyPaymentBulkSaveItemRequest item : request.getItems()) {
            if (item.isDeleted()) {
                if (item.getId() != null) {
                    DailyPayment entity = findPayment(item.getId());
                    entity.setDeletedAt(Instant.now());
                }
                continue;
            }

            if (item.isNew()) {
                createPayment(request.getPaymentDate(), item);
                continue;
            }

            if (item.isUpdated()) {
                if (item.getId() == null) {
                    continue;
                }

                updatePayment(item.getId(), item);
            }
        }

        return findByPaymentDate(request.getPaymentDate());
    }

    private DailyPayment createPayment(
            LocalDate paymentDate,
            DailyPaymentBulkSaveItemRequest item
    ) {
        DailyPayment entity = dailyPaymentRepository
                .findByPaymentDateAndEmployeeIdAndDeletedAtIsNull(
                        paymentDate,
                        item.getEmployeeId()
                )
                .orElseGet(DailyPayment::new);

        entity.setPaymentDate(paymentDate);
        entity.setEmployeeId(item.getEmployeeId());

        applyPaymentAmounts(entity, item);

        return dailyPaymentRepository.save(entity);
    }

    @SuppressWarnings("null")
    private DailyPayment updatePayment(
            Long id,
            DailyPaymentBulkSaveItemRequest item
    ) {
        DailyPayment entity = findPayment(id);

        applyPaymentAmounts(entity, item);

        return dailyPaymentRepository.save(entity);
    }

    private void applyPaymentAmounts(
            DailyPayment entity,
            DailyPaymentBulkSaveItemRequest item
    ) {
        entity.setPlannedAmount(nvl(item.getPlannedAmount()));
        entity.setActualAmount(nvl(item.getActualAmount()));

        DailyPaymentStatus oldStatus = entity.getStatus();

        entity.setStatus(
                item.getStatus() != null
                        ? item.getStatus()
                        : DailyPaymentStatus.PENDING
        );

        if (entity.getStatus() == DailyPaymentStatus.PAID && oldStatus != DailyPaymentStatus.PAID) {
            entity.setPaidAt(Instant.now());
        }

        if (entity.getStatus() != DailyPaymentStatus.PAID) {
            entity.setPaidAt(null);
        }

        entity.setNote(item.getNote());
    }

    private DailyPayment createGeneratedPayment(
            LocalDate paymentDate,
            DailyReport report
    ) {
        DailyPayment entity = new DailyPayment();

        entity.setId(null);
        entity.setPaymentDate(paymentDate);

        entity.setEmployeeId(report.getEmployee().getId());
        entity.setEmployeeCode(report.getEmployee().getEmployeeCode());
        entity.setEmployeeName(report.getEmployee().getEmployeeName());

        BigDecimal plannedAmount = calculatePlannedAmount(report);

        entity.setPlannedAmount(plannedAmount);
        entity.setActualAmount(plannedAmount);
        entity.setStatus(DailyPaymentStatus.PENDING);
        entity.setNote(null);

        return entity;
    }

    private BigDecimal calculatePlannedAmount(DailyReport report) {
        EmployeeContract contract = employeeContractRepository
                .findByEmployeeIdAndDeletedAtIsNull(report.getEmployee().getId())
                .orElse(null);

        BigDecimal basePay = BigDecimal.ZERO;

        if (contract != null && contract.getSalaryType() != null) {
            SalaryType salaryType = contract.getSalaryType();

            if (salaryType == SalaryType.DAILY) {
                basePay = nvl(contract.getDailyWage());
            }

            if (salaryType == SalaryType.HOURLY) {
                BigDecimal totalHours = nvl(report.getWorkHours())
                        .add(nvl(report.getOvertimeHours()))
                        .add(nvl(report.getNightWorkHours()));

                basePay = nvl(contract.getHourlyWage()).multiply(totalHours);
            }
        }

        return basePay
                .add(nvl(report.getAllowanceAmount()))
                .subtract(nvl(report.getDeductionAmount()))
                .subtract(nvl(report.getSavingAmount()))
                .subtract(nvl(report.getLoanRepaymentAmount()));
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

    private DailyPayment findPayment(Long id) {
        return dailyPaymentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("日払いデータが見つかりません。 id=" + id));
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}