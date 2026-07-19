package com.project.backend.features.system.report.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.report.dto.ReportHistoryDetailResponse;
import com.project.backend.features.system.report.dto.ReportHistoryResponse;
import com.project.backend.features.system.report.service.admin.ReportHistoryAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/report-histories")
@RequiredArgsConstructor
public class ReportHistoryAdminController {

    private final ReportHistoryAdminService reportHistoryAdminService;

    @GetMapping
    public List<ReportHistoryResponse> findAll() {
        return reportHistoryAdminService.findAll();
    }

    @GetMapping("/by-report-master/{reportMasterId}")
    public List<ReportHistoryResponse> findByReportMasterId(@PathVariable Long reportMasterId) {
        return reportHistoryAdminService.findByReportMasterId(reportMasterId);
    }

    @GetMapping("/{id}")
    public ReportHistoryDetailResponse findById(@PathVariable Long id) {
        return reportHistoryAdminService.findById(id);
    }
}