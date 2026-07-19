package com.project.backend.features.system.report.service.core;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.service.api.exporter.ReportExporterResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportFileGenerationService {

    private final ReportExporterResolver exporterResolver;

    public FileExportResult generate(
            ReportMaster reportMaster,
            String executionId,
            List<Map<String, Object>> rows
    ) {
        return exporterResolver
                .resolve(reportMaster)
                .export(reportMaster, executionId, rows);
    }
}
