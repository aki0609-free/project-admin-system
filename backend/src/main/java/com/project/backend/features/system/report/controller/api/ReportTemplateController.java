package com.project.backend.features.system.report.controller.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.report.dto.ReportTemplateResponse;
import com.project.backend.features.system.report.service.api.ReportTemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/report-templates")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class ReportTemplateController {

    private final ReportTemplateService reportTemplateService;

    @GetMapping
    public List<ReportTemplateResponse> findAll() {
        return reportTemplateService.findAll();
    }
}
