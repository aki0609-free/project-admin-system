package com.project.backend.features.system.report.service.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.dto.ReportMasterSaveRequest;
import com.project.backend.features.system.report.enums.ReportCleanupType;
import com.project.backend.features.system.report.enums.ReportPreProcessType;

@Component
public class ReportMasterValidator {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,199}$");
    private static final Pattern SHA_256 =
            Pattern.compile("^[A-Fa-f0-9]{64}$");

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
        validateSnapshotDefinition(request);
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

    private void validateSnapshotDefinition(
            ReportMasterSaveRequest request
    ) {
        validateOptionalIdentifier(
                "sourceViewName",
                request.sourceViewName()
        );
        validateOptionalIdentifier(
                "historyTable",
                request.historyTable()
        );

        if (StringUtils.hasText(request.htmlTemplateKey())) {
            String key = request.htmlTemplateKey();
            if (key.startsWith("/")
                    || key.contains("..")
                    || key.contains("\\")
                    || !key.endsWith(".html")) {
                throw new RuntimeException(
                        "htmlTemplateKeyは安全な.htmlのS3キーを指定してください。"
                );
            }
        }
        if (request.htmlTemplateVersion() != null
                && request.htmlTemplateVersion() < 1) {
            throw new RuntimeException(
                    "htmlTemplateVersionは1以上で指定してください。"
            );
        }
        if (StringUtils.hasText(request.htmlTemplateHash())
                && !SHA_256.matcher(
                        request.htmlTemplateHash()
                ).matches()) {
            throw new RuntimeException(
                    "htmlTemplateHashはSHA-256形式で指定してください。"
            );
        }
    }

    private void validateOptionalIdentifier(
            String fieldName,
            String value
    ) {
        if (StringUtils.hasText(value)
                && !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new RuntimeException(
                    fieldName + "に不正な識別子が指定されています。"
            );
        }
    }
}
