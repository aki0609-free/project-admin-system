package com.project.backend.features.system.report.service.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.dto.ReportMasterSaveRequest;
import com.project.backend.features.system.report.enums.ReportCleanupType;
import com.project.backend.features.system.report.enums.ReportOutputFormat;
import com.project.backend.features.system.report.enums.ReportPreProcessType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportMasterValidator {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,199}$");
    private static final Pattern SHA_256 =
            Pattern.compile("^[A-Fa-f0-9]{64}$");
    private static final Pattern REPORT_CODE =
            Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,99}$");

    private final ReportTemplateValidator reportTemplateValidator;

    public void validate(ReportMasterSaveRequest request) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.reportCode())) {
            throw new RuntimeException("reportCode は必須です。");
        }
        if (!REPORT_CODE.matcher(request.reportCode()).matches()) {
            throw new RuntimeException(
                    "reportCode は半角英大文字、数字、_、-で指定してください。"
            );
        }

        if (!StringUtils.hasText(request.reportName())) {
            throw new RuntimeException("reportName は必須です。");
        }

        if (!StringUtils.hasText(request.workTable())) {
            throw new RuntimeException("workTable は必須です。");
        }

        validateOptionalIdentifier("workTable", request.workTable());
        validateOptionalIdentifier("inputTable", request.inputTable());
        validateOptionalIdentifier("outputTable", request.outputTable());
        validateOptionalIdentifier("procedureName", request.procedureName());
        validateOptionalIdentifier(
                "cleanupProcedureName",
                request.cleanupProcedureName()
        );

        validatePreProcess(request);
        validateCleanup(request);
        validateOutputDefinition(request);
        validateSnapshotDefinition(request);
    }

    private void validateOutputDefinition(ReportMasterSaveRequest request) {
        ReportOutputFormat outputFormat = request.outputFormat();
        if (outputFormat == null) {
            throw new RuntimeException("outputFormat は必須です。");
        }

        if (outputFormat == ReportOutputFormat.PDF) {
            reportTemplateValidator.validateJrxmlFileName(
                    request.templateFileName()
            );
            return;
        }

        if (outputFormat == ReportOutputFormat.EXCEL
                && StringUtils.hasText(request.templateFileName())) {
            reportTemplateValidator.validateExcelFileName(
                    request.templateFileName()
            );
            return;
        }

        if (outputFormat == ReportOutputFormat.CSV
                && StringUtils.hasText(request.templateFileName())) {
            throw new RuntimeException(
                    "CSV帳票にはテンプレートファイルを指定できません。"
            );
        }
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
