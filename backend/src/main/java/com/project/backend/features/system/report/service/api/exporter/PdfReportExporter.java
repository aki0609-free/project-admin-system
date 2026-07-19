package com.project.backend.features.system.report.service.api.exporter;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportOutputFormat;
import com.project.backend.features.system.report.service.api.ReportPdfRenderer;
import com.project.backend.features.system.report.service.builder.ReportFileNameBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfReportExporter implements ReportExporter {

    private final ReportPdfRenderer reportPdfRenderer;
    private final ReportFileNameBuilder fileNameBuilder;

    @Override
    public boolean supports(ReportMaster reportMaster) {
        return reportMaster.getOutputFormat() == ReportOutputFormat.PDF;
    }

    @Override
    public FileExportResult export(
            ReportMaster reportMaster,
            String executionId,
            List<Map<String, Object>> rows
    ) {

        byte[] data = reportPdfRenderer.render(
                reportMaster,
                Map.of(
                        "reportCode", reportMaster.getReportCode(),
                        "executionId", executionId
                ),
                rows
        );

        return new FileExportResult(
                fileNameBuilder.build(reportMaster, "pdf"),
                "application/pdf",
                data
        );
    }
}