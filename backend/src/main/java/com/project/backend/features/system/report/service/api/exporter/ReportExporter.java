package com.project.backend.features.system.report.service.api.exporter;

import java.util.List;
import java.util.Map;

import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.entity.ReportMaster;

public interface ReportExporter {

    boolean supports(ReportMaster reportMaster);

    FileExportResult export(
            ReportMaster reportMaster,
            String executionId,
            List<Map<String, Object>> rows
    );
}