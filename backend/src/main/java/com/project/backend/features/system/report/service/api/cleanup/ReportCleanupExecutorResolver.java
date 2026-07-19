package com.project.backend.features.system.report.service.api.cleanup;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.enums.ReportCleanupType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportCleanupExecutorResolver {

    private final List<ReportCleanupExecutor> executors;

    public ReportCleanupExecutor resolve(ReportCleanupType type) {
        ReportCleanupType resolvedType =
                type != null ? type : ReportCleanupType.NONE;

        return executors.stream()
                .filter(executor -> executor.supportedType() == resolvedType)
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("未対応のクリーンアップタイプです。 type=" + resolvedType)
                );
    }
}