package com.project.backend.features.system.report.controller.admin;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.report.dto.ReportTestParamTemplateResponse;
import com.project.backend.features.system.report.dto.ReportTestParamTemplateSaveRequest;
import com.project.backend.features.system.report.service.admin.ReportTestParamTemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/report-test-param-templates")
@RequiredArgsConstructor
public class ReportTestParamTemplateController {

    private final ReportTestParamTemplateService service;

    @GetMapping
    public List<ReportTestParamTemplateResponse> findByReportCode(
            @RequestParam String reportCode
    ) {
        return service.findByReportCode(reportCode);
    }

    @GetMapping("/default")
    public ReportTestParamTemplateResponse findDefault(
            @RequestParam String reportCode
    ) {
        return service.findDefault(reportCode);
    }

    @PostMapping
    public ReportTestParamTemplateResponse create(
            @RequestBody ReportTestParamTemplateSaveRequest request
    ) {
        return service.create(request);
    }
}