package com.project.backend.features.operation.reportpreview.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.operation.reportpreview.dto.OperationReportPreviewHtmlRequest;
import com.project.backend.features.operation.reportpreview.dto.OperationReportPreviewResponse;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.service.OperationReportPreviewHtmlService;
import com.project.backend.features.operation.reportpreview.service.OperationReportPreviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/operation/report-previews")
@RequiredArgsConstructor
public class OperationReportPreviewController {

    private final OperationReportPreviewService service;
    private final OperationReportPreviewHtmlService htmlService;

    @GetMapping
    public List<OperationReportPreviewResponse> findReports(
            @RequestParam OperationType operationType) {
        return service.findReports(operationType);
    }

    @GetMapping(
            value = "/html",
            produces = MediaType.TEXT_HTML_VALUE
    )
    public String previewHtml(
            @RequestParam OperationType operationType,
            @RequestParam String reportCode,
            @RequestParam(required = false) String targetDate,
            @RequestParam(required = false) String targetMonth) {

        return htmlService.renderHtml(
                new OperationReportPreviewHtmlRequest(
                        operationType,
                        reportCode,
                        targetDate,
                        targetMonth));
    }
}