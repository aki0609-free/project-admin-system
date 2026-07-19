package com.project.backend.features.system.report.service.api;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.service.core.ReportDefinitionService;
import com.project.backend.features.system.report.service.loader.ReportOutputRowLoader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportPreviewService {

    private final ReportDefinitionService reportDefinitionService;
    private final ReportOutputRowLoader rowLoader;
    private final ReportPdfRenderer reportPdfRenderer;

    public byte[] previewPdf(String reportCode, String executionId) {
        ReportMaster reportMaster =
                reportDefinitionService.getActiveReport(reportCode);

        List<Map<String, Object>> rows =
                rowLoader.loadRows(reportMaster, executionId);

        return reportPdfRenderer.render(
                reportMaster,
                Map.of(
                        "reportCode", reportCode,
                        "executionId", executionId
                ),
                rows
        );
    }
}