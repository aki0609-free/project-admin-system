package com.project.backend.features.dailyreport.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.dailyreport.dto.DailyReportDetailResponse;
import com.project.backend.features.dailyreport.dto.DailyReportEstimatedPayPreviewResponse;
import com.project.backend.features.dailyreport.dto.DailyReportMissingEmployeeResponse;
import com.project.backend.features.dailyreport.dto.DailyReportMonthlyAttendanceResponse;
import com.project.backend.features.dailyreport.dto.DailyReportResponse;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.service.DailyReportCommandService;
import com.project.backend.features.dailyreport.service.DailyReportEstimatedPayService;
import com.project.backend.features.dailyreport.service.DailyReportMissingQueryService;
import com.project.backend.features.dailyreport.service.DailyReportMonthlyAttendanceQueryService;
import com.project.backend.features.dailyreport.service.DailyReportQueryService;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.repository.EmployeeContractRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/daily-reports")
@RequiredArgsConstructor
public class DailyReportController {

    private final DailyReportQueryService queryService;
    private final DailyReportCommandService commandService;
    private final DailyReportMissingQueryService missingQueryService;
    private final DailyReportMonthlyAttendanceQueryService monthlyAttendanceQueryService;
    private final DailyReportEstimatedPayService estimatedPayService;
    private final EmployeeContractRepository employeeContractRepository;

    @GetMapping
    public List<DailyReportResponse> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long employeeId
    ) {
        return queryService.findAll(from, to, employeeId);
    }

    @GetMapping("/{id}")
    public DailyReportDetailResponse findDetail(
            @PathVariable Long id
    ) {
        return queryService.findDetail(id);
    }

    @PostMapping
    public DailyReportResponse create(
            @RequestBody DailyReportSaveRequest request
    ) {
        return commandService.create(request);
    }

    @PutMapping("/{id}")
    public DailyReportResponse update(
            @PathVariable Long id,
            @RequestBody DailyReportSaveRequest request
    ) {
        return commandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id
    ) {
        commandService.delete(id);
    }

    @GetMapping("/missing")
    public List<DailyReportMissingEmployeeResponse> findMissingEmployees(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate
    ) {
        return missingQueryService.findMissingEmployees(workDate);
    }

    @GetMapping("/monthly-attendance")
    public List<DailyReportMonthlyAttendanceResponse> findMonthlyAttendance(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth targetMonth
    ) {
        return monthlyAttendanceQueryService.findMonthlyAttendance(targetMonth);
    }

    @PostMapping("/estimated-pay-preview")
    public DailyReportEstimatedPayPreviewResponse previewEstimatedPay(
            @RequestBody DailyReportSaveRequest request
    ) {
        if (request == null || request.employeeId() == null) {
            return DailyReportEstimatedPayPreviewResponse.builder()
                    .estimatedBasePayAmount(BigDecimal.ZERO)
                    .estimatedGrossPayAmount(BigDecimal.ZERO)
                    .estimatedNetPayAmount(BigDecimal.ZERO)
                    .build();
        }

        EmployeeContract contract =
                employeeContractRepository
                        .findByEmployeeIdAndDeletedAtIsNull(request.employeeId())
                        .orElse(null);

        return estimatedPayService.preview(request, contract);
    }
}