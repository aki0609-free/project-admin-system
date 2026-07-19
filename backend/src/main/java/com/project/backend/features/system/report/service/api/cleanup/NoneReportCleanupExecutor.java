package com.project.backend.features.system.report.service.api.cleanup;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportCleanupType;

@Component
public class NoneReportCleanupExecutor implements ReportCleanupExecutor {

    @Override
    public ReportCleanupType supportedType() {
        return ReportCleanupType.NONE;
    }

    @Override
    public void execute(ReportMaster reportMaster, String executionId) {
        // 何もしない
    }
}