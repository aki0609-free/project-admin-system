package com.project.backend.features.system.report.dto;

import com.project.backend.features.system.report.enums.ReportOutputFileStatus;

public record ReportRecipientFileProcessResult(
        ReportOutputFileStatus status,
        boolean skipped,
        String mailType
) {
    public static ReportRecipientFileProcessResult created(String mailType) {
        return new ReportRecipientFileProcessResult(
                ReportOutputFileStatus.CREATED,
                false,
                mailType
        );
    }

    public static ReportRecipientFileProcessResult failed(String mailType) {
        return new ReportRecipientFileProcessResult(
                ReportOutputFileStatus.FAILED,
                false,
                mailType
        );
    }

    public static ReportRecipientFileProcessResult skippedResult() {
        return new ReportRecipientFileProcessResult(null, true, null);
    }
}
