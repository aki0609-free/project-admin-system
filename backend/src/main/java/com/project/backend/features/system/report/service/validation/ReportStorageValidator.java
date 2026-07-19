package com.project.backend.features.system.report.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.entity.ReportMaster;

@Component
public class ReportStorageValidator {

    public void validateSaveRequest(
            ReportMaster reportMaster,
            String executionId,
            FileExportResult result
    ) {
        if (reportMaster == null) {
            throw new RuntimeException("reportMaster が未設定です。");
        }

        if (!StringUtils.hasText(reportMaster.getReportCode())) {
            throw new RuntimeException("reportCode が未設定です。");
        }

        if (!StringUtils.hasText(executionId)) {
            throw new RuntimeException("executionId が未設定です。");
        }

        if (result == null) {
            throw new RuntimeException("出力結果が未設定です。");
        }

        if (!StringUtils.hasText(result.fileName())) {
            throw new RuntimeException("fileName が未設定です。");
        }

        if (result.data() == null || result.data().length == 0) {
            throw new RuntimeException("帳票データが空です。");
        }
    }

    public void validateLoadRequest(
            StorageType storageType,
            String fileKey
    ) {
        if (storageType == null) {
            throw new RuntimeException("storageType が未設定です。");
        }

        if (!StringUtils.hasText(fileKey)) {
            throw new RuntimeException("fileKey が未設定です。");
        }
    }
}