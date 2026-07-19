package com.project.backend.features.system.report.service.api;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.service.api.cleanup.ReportCleanupExecutorResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportCleanupService {

    private final ReportCleanupExecutorResolver cleanupExecutorResolver;

    public void cleanup(ReportMaster reportMaster, String executionId) {
        cleanupExecutorResolver
                .resolve(reportMaster.getCleanupType())
                .execute(reportMaster, executionId);
    }
}