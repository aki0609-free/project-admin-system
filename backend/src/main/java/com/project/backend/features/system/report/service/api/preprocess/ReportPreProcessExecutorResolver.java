package com.project.backend.features.system.report.service.api.preprocess;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.enums.ReportPreProcessType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportPreProcessExecutorResolver {

    private final List<ReportPreProcessExecutor> executors;

    public ReportPreProcessExecutor resolve(ReportPreProcessType type) {
        ReportPreProcessType resolvedType =
                type != null ? type : ReportPreProcessType.NONE;

        return executors.stream()
                .filter(executor -> executor.supportedType() == resolvedType)
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("未対応の前処理タイプです。 type=" + resolvedType)
                );
    }
}