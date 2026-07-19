package com.project.backend.features.system.report.service.api.preprocess;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.enums.ReportPreProcessType;

public interface ReportPreProcessExecutor {

    ReportPreProcessType supportedType();

    void execute(ReportMaster reportMaster, String executionId);
}