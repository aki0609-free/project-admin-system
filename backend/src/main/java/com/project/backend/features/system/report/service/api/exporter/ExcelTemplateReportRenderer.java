package com.project.backend.features.system.report.service.api.exporter;

import java.util.List;
import java.util.Map;

import com.project.backend.features.system.report.entity.ReportMaster;

public interface ExcelTemplateReportRenderer {

    boolean supports(ReportMaster reportMaster);

    default boolean fallback() {
        return false;
    }

    byte[] render(
            ReportMaster reportMaster,
            byte[] template,
            List<Map<String, Object>> rows
    );
}
