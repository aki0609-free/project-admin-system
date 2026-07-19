package com.project.backend.features.system.report.controller.api;

import java.util.Base64;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.dto.ReportPreviewResponse;
import com.project.backend.features.system.report.service.api.ReportExportService;
import com.project.backend.features.system.report.service.api.ReportPreviewService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/report-export")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportPreviewService reportPreviewService;
    private final ReportExportService reportExportService;

    @GetMapping("/{reportCode}/preview")
    public ReportPreviewResponse preview(
            @PathVariable String reportCode,
            @RequestParam String executionId
    ) {
        byte[] pdf = reportPreviewService.previewPdf(reportCode, executionId);

        return ReportPreviewResponse.builder()
                .reportCode(reportCode)
                .executionId(executionId)
                .contentType("application/pdf")
                .fileName(reportCode + ".pdf")
                .base64Data(Base64.getEncoder().encodeToString(pdf))
                .build();
    }

    @GetMapping("/{reportCode}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable String reportCode,
            @RequestParam String executionId
    ) {
        FileExportResult result = reportExportService.export(reportCode, executionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(result.fileName())
                        .build()
        );
        headers.add(HttpHeaders.CONTENT_TYPE, result.contentType());

        return ResponseEntity.ok()
                .headers(headers)
                .body(result.data());
    }
}