package com.project.backend.features.system.report.service.api.cleanup;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportCleanupType;

public interface ReportCleanupExecutor {

    ReportCleanupType supportedType();

    void execute(ReportMaster reportMaster, String executionId);
}