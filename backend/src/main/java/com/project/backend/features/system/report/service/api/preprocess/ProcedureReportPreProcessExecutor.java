package com.project.backend.features.system.report.service.api.preprocess;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.sql.service.ProcedureExecutorService;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportPreProcessType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProcedureReportPreProcessExecutor implements ReportPreProcessExecutor {

    private final ProcedureExecutorService procedureExecutorService;

    @Override
    public ReportPreProcessType supportedType() {
        return ReportPreProcessType.PROCEDURE;
    }

    @Override
    public void execute(ReportMaster reportMaster, String executionId) {
        if (!StringUtils.hasText(reportMaster.getProcedureName())) {
            throw new RuntimeException("preProcessType=PROCEDURE ですが procedureName が未設定です。");
        }

        procedureExecutorService.executeByExecutionId(
                reportMaster.getProcedureName(),
                executionId
        );
    }
}