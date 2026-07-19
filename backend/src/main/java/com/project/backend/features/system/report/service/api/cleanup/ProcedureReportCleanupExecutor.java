package com.project.backend.features.system.report.service.api.cleanup;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.sql.service.ProcedureExecutorService;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportCleanupType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProcedureReportCleanupExecutor implements ReportCleanupExecutor {

    private final ProcedureExecutorService procedureExecutorService;

    @Override
    public ReportCleanupType supportedType() {
        return ReportCleanupType.PROCEDURE;
    }

    @Override
    public void execute(ReportMaster reportMaster, String executionId) {
        if (!StringUtils.hasText(reportMaster.getCleanupProcedureName())) {
            throw new RuntimeException("cleanupType=PROCEDURE ですが cleanupProcedureName が未設定です。");
        }

        procedureExecutorService.executeByExecutionId(
                reportMaster.getCleanupProcedureName(),
                executionId
        );
    }
}