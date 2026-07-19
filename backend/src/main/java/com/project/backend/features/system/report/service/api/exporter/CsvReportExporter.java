package com.project.backend.features.system.report.service.api.exporter;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.app.file.service.CsvFileWriter;
import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportOutputFormat;
import com.project.backend.features.system.report.service.builder.ReportFileNameBuilder;
import com.project.backend.features.system.report.service.loader.ReportOutputRowLoader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvReportExporter implements ReportExporter {

    private final CsvFileWriter csvFileWriter;
    private final ReportOutputRowLoader rowLoader;
    private final ReportFileNameBuilder fileNameBuilder;

    @Override
    public boolean supports(ReportMaster reportMaster) {
        return reportMaster.getOutputFormat() == ReportOutputFormat.CSV;
    }

    @Override
    public FileExportResult export(
            ReportMaster reportMaster,
            String executionId,
            List<Map<String, Object>> rows
    ) {

        List<String> headers =
                rowLoader.extractHeaders(rows);

        byte[] data =
                csvFileWriter.write(rows, headers);

        return new FileExportResult(
                fileNameBuilder.build(reportMaster, "csv"),
                "text/csv; charset=UTF-8",
                data
        );
    }
}
