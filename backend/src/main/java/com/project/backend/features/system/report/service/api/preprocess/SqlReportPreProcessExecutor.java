package com.project.backend.features.system.report.service.api.preprocess;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportPreProcessType;
import com.project.backend.features.system.report.service.api.ReportSqlExecutorService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SqlReportPreProcessExecutor implements ReportPreProcessExecutor {

    private final ReportSqlExecutorService reportSqlExecutorService;

    @Override
    public ReportPreProcessType supportedType() {
        return ReportPreProcessType.SQL;
    }

    @Override
    public void execute(ReportMaster reportMaster, String executionId) {
        if (!StringUtils.hasText(reportMaster.getPreProcessSql())) {
            throw new RuntimeException("preProcessType=SQL ですが preProcessSql が未設定です。");
        }

        reportSqlExecutorService.executeByExecutionId(
                reportMaster.getPreProcessSql(),
                executionId
        );
    }
}