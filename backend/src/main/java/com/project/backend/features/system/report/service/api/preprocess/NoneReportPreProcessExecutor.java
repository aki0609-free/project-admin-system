package com.project.backend.features.system.report.service.api.preprocess;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportPreProcessType;

@Component
public class NoneReportPreProcessExecutor implements ReportPreProcessExecutor {

    @Override
    public ReportPreProcessType supportedType() {
        return ReportPreProcessType.NONE;
    }

    @Override
    public void execute(ReportMaster reportMaster, String executionId) {
        // 何もしない
    }
}