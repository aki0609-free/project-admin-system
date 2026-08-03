package com.project.backend.features.system.report.service.api.exporter;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.app.file.service.ExcelFileWriter;
import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportOutputFormat;
import com.project.backend.features.system.report.service.builder.ReportFileNameBuilder;
import com.project.backend.features.system.report.service.loader.ReportOutputRowLoader;
import com.project.backend.features.system.report.service.loader.ReportTemplateLoader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExcelReportExporter implements ReportExporter {

    private final ExcelFileWriter excelFileWriter;
    private final ReportOutputRowLoader rowLoader;
    private final ReportFileNameBuilder fileNameBuilder;
    private final ReportTemplateLoader reportTemplateLoader;
    private final ExcelTemplateReportRendererRegistry templateRendererRegistry;

    @Override
    public boolean supports(ReportMaster reportMaster) {
        return reportMaster.getOutputFormat() == ReportOutputFormat.EXCEL;
    }

    @Override
    public FileExportResult export(
            ReportMaster reportMaster,
            String executionId,
            List<Map<String, Object>> rows
    ) {

        byte[] data;
        if (reportMaster.getTemplateFileName() != null
                && reportMaster.getTemplateFileName()
                        .toLowerCase()
                        .endsWith(".xlsx")) {
            data = templateRendererRegistry.resolve(reportMaster).render(
                    reportMaster,
                    reportTemplateLoader.loadExcel(
                            reportMaster.getTemplateFileName()
                    ),
                    rows
            );
        } else {
            List<String> headers = rowLoader.extractHeaders(rows);
            data = excelFileWriter.write(
                    rows,
                    headers,
                    reportMaster.getReportCode()
            );
        }

        return new FileExportResult(
                fileNameBuilder.build(reportMaster, "xlsx"),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                data
        );
    }
}
