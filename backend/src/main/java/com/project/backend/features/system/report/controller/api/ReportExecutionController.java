package com.project.backend.features.system.report.controller.api;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.report.dto.ReportExecuteRequest;
import com.project.backend.features.system.report.dto.ReportExecuteResponse;
import com.project.backend.features.system.report.service.api.ReportExecutionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/report-execution")
@RequiredArgsConstructor
public class ReportExecutionController {

    private final ReportExecutionService reportExecutionService;

    @PostMapping("/{reportCode}/prepare")
    public ReportExecuteResponse prepare(
            @PathVariable String reportCode,
            @RequestBody(required = false) ReportExecuteRequest request
    ) {
        return reportExecutionService.prepareExecution(
                reportCode,
                request != null ? request.params() : null
        );
    }
}