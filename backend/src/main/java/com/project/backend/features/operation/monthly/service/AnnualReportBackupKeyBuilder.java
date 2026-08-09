package com.project.backend.features.operation.monthly.service;

import org.springframework.stereotype.Component;

import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingReportFile;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnnualReportBackupKeyBuilder {

    private final DocumentStorageKeyResolver keyResolver;

    public String build(
            String tenantId,
            int fiscalYear,
            MonthlyClosingReportFile source
    ) {
        return keyResolver.resolve(
                DocumentArea.BACKUPS,
                "reports/"
                        + safeSegment(tenantId)
                        + "/"
                        + fiscalYear
                        + "/"
                        + safeSegment(source.getReportCode())
                        + "/"
                        + safeSegment(source.getTargetMonth())
                        + "/v"
                        + source.getClosingVersion()
                        + "/"
                        + source.getId()
                        + "-"
                        + safeFileName(source.getOutputFileName())
        );
    }

    private String safeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim()
                .replace("\\", "_")
                .replace("/", "_")
                .replace("..", "_");
    }

    private String safeFileName(String value) {
        if (value == null || value.isBlank()) {
            return "report.bin";
        }
        return safeSegment(value)
                .replace("\r", "_")
                .replace("\n", "_");
    }
}
