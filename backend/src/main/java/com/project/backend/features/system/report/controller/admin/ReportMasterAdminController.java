package com.project.backend.features.system.report.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.report.dto.ReportMasterDetailResponse;
import com.project.backend.features.system.report.dto.ReportMasterListResponse;
import com.project.backend.features.system.report.dto.ReportMasterSaveRequest;
import com.project.backend.features.system.report.service.admin.ReportMasterAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/report-masters")
@RequiredArgsConstructor
public class ReportMasterAdminController {

    private final ReportMasterAdminService reportMasterAdminService;

    @GetMapping
    public List<ReportMasterListResponse> findAll() {
        return reportMasterAdminService.findAll();
    }

    @GetMapping("/{id}")
    public ReportMasterDetailResponse findDetail(@PathVariable Long id) {
        return reportMasterAdminService.findDetail(id);
    }

    @PostMapping
    public ReportMasterDetailResponse create(@RequestBody ReportMasterSaveRequest request) {
        return reportMasterAdminService.create(request);
    }

    @PutMapping("/{id}")
    public ReportMasterDetailResponse update(
            @PathVariable Long id,
            @RequestBody ReportMasterSaveRequest request
    ) {
        return reportMasterAdminService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        reportMasterAdminService.delete(id);
    }
}