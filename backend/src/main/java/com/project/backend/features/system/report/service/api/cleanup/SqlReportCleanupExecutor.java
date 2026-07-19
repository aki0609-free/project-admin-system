package com.project.backend.features.system.report.service.api.cleanup;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportCleanupType;
import com.project.backend.features.system.report.service.api.ReportSqlExecutorService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SqlReportCleanupExecutor implements ReportCleanupExecutor {

    private final ReportSqlExecutorService reportSqlExecutorService;

    @Override
    public ReportCleanupType supportedType() {
        return ReportCleanupType.SQL;
    }

    @Override
    public void execute(ReportMaster reportMaster, String executionId) {
        if (!StringUtils.hasText(reportMaster.getCleanupSql())) {
            throw new RuntimeException("cleanupType=SQL ですが cleanupSql が未設定です。");
        }

        reportSqlExecutorService.execute(
                reportMaster.getCleanupSql(),
                Map.of("executionId", executionId)
        );
    }
}