package com.project.backend.features.system.report.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.dto.ReportMasterSaveRequest;
import com.project.backend.features.system.report.enums.ReportCleanupType;
import com.project.backend.features.system.report.enums.ReportPreProcessType;

@Component
public class ReportMasterValidator {

    public void validate(ReportMasterSaveRequest request) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.reportCode())) {
            throw new RuntimeException("reportCode は必須です。");
        }

        if (!StringUtils.hasText(request.reportName())) {
            throw new RuntimeException("reportName は必須です。");
        }

        if (!StringUtils.hasText(request.workTable())) {
            throw new RuntimeException("workTable は必須です。");
        }

        validatePreProcess(request);
        validateCleanup(request);
    }

    private void validatePreProcess(ReportMasterSaveRequest request) {
        ReportPreProcessType type = request.preProcessType();

        if (type == ReportPreProcessType.SQL
                && !StringUtils.hasText(request.preProcessSql())) {
            throw new RuntimeException("preProcessType=SQL の場合、preProcessSql は必須です。");
        }

        if (type == ReportPreProcessType.PROCEDURE
                && !StringUtils.hasText(request.procedureName())) {
            throw new RuntimeException("preProcessType=PROCEDURE の場合、procedureName は必須です。");
        }
    }

    private void validateCleanup(ReportMasterSaveRequest request) {
        ReportCleanupType type = request.cleanupType();

        if (type == ReportCleanupType.SQL
                && !StringUtils.hasText(request.cleanupSql())) {
            throw new RuntimeException("cleanupType=SQL の場合、cleanupSql は必須です。");
        }

        if (type == ReportCleanupType.PROCEDURE
                && !StringUtils.hasText(request.cleanupProcedureName())) {
            throw new RuntimeException("cleanupType=PROCEDURE の場合、cleanupProcedureName は必須です。");
        }
    }
}