package com.project.backend.features.system.report.service.builder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.features.system.report.entity.ReportMaster;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportFileKeyBuilder {

    private final StorageProperties properties;

    public String build(
            ReportMaster reportMaster,
            String executionId,
            String fileName) {
        String basePath = properties.getOutput().getPath();

        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        return basePath
                + safeSegment(reportMaster.getReportCode())
                + "/"
                + safeSegment(executionId)
                + "/"
                + safeFileName(fileName);
    }

    public String buildRecipient(
            ReportMaster reportMaster,
            String executionId,
            String businessKey,
            String fileName
    ) {
        String basePath = properties.getOutput().getPath();

        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        return basePath
                + safeSegment(reportMaster.getReportCode())
                + "/"
                + safeSegment(executionId)
                + "/recipients/"
                + opaqueGroupId(businessKey)
                + "/"
                + safeFileName(fileName);
    }

    private String opaqueGroupId(String businessKey) {
        if (!StringUtils.hasText(businessKey)) {
            throw new RuntimeException("businessKey が未設定です。");
        }

        return UUID.nameUUIDFromBytes(
                businessKey.getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    private String safeSegment(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }

        return value
                .replace("\\", "_")
                .replace("/", "_")
                .replace("..", "_")
                .trim();
    }

    private String safeFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return "report.pdf";
        }

        return value
                .replace("\\", "_")
                .replace("/", "_")
                .replace("\r", "_")
                .replace("\n", "_")
                .replace("..", "_")
                .trim();
    }
}
