package com.project.backend.features.operation.monthly.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.operation.monthly.dto.MonthlyClosingResponse;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingReportFileResponse;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingSummaryResponse;
import com.project.backend.features.operation.monthly.service.MonthlyClosingCommandService;
import com.project.backend.features.operation.monthly.service.MonthlyClosingReportFileService;
import com.project.backend.features.operation.monthly.service.MonthlySummaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/operation/monthly")
@RequiredArgsConstructor
public class MonthlyOperationController {

    private final MonthlySummaryService summaryService;
    private final MonthlyClosingCommandService closingCommandService;
    private final MonthlyClosingReportFileService reportFileService;

    @GetMapping("/summary")
    public MonthlyClosingSummaryResponse findSummary(
            @RequestParam String targetMonth) {
        return summaryService.findSummary(targetMonth);
    }

    @PostMapping("/close")
    public MonthlyClosingResponse close(
            @RequestParam String targetMonth) {
        return closingCommandService.close(targetMonth);
    }

    @PostMapping("/reclose")
    public MonthlyClosingResponse reclose(
            @RequestParam String targetMonth) {
        return closingCommandService.reclose(targetMonth);
    }

    @GetMapping("/report-files")
    public List<MonthlyClosingReportFileResponse> findReportFiles(
            @RequestParam String targetMonth,
            @RequestParam(required = false) Integer closingVersion,
            @RequestParam String reportCode) {
        return reportFileService.findAll(targetMonth, closingVersion, reportCode);
    }
}
