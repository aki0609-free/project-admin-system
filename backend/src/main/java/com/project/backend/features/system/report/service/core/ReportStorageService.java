package com.project.backend.features.system.report.service.core;

import java.io.ByteArrayInputStream;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.report.dto.FileExportResult;
import com.project.backend.features.system.report.dto.ReportStoredFile;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.service.builder.ReportFileKeyBuilder;
import com.project.backend.features.system.report.service.validation.ReportStorageValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportStorageService {

    private final StorageService storageService;
    private final ReportFileKeyBuilder fileKeyBuilder;
    private final ReportStorageValidator validator;

    public ReportStoredFile save(
            ReportMaster reportMaster,
            String executionId,
            FileExportResult result
    ) {
        validator.validateSaveRequest(reportMaster, executionId, result);

        try {
            String fileKey = fileKeyBuilder.build(
                    reportMaster,
                    executionId,
                    result.fileName()
            );

            StorageType storageType = storageService.type();

            storageService.save(
                    storageType,
                    fileKey,
                    new ByteArrayInputStream(result.data()),
                    result.data().length,
                    result.contentType()
            );

            return new ReportStoredFile(
                    storageType,
                    fileKey,
                    result.fileName(),
                    result.contentType(),
                    (long) result.data().length
            );

        } catch (Exception e) {
            throw new RuntimeException("帳票ファイル保存に失敗しました。", e);
        }
    }

    public ReportStoredFile saveRecipient(
            ReportMaster reportMaster,
            String executionId,
            String businessKey,
            FileExportResult result
    ) {
        validator.validateSaveRequest(reportMaster, executionId, result);

        try {
            String fileKey = fileKeyBuilder.buildRecipient(
                    reportMaster,
                    executionId,
                    businessKey,
                    result.fileName()
            );

            StorageType storageType = storageService.type();

            storageService.save(
                    storageType,
                    fileKey,
                    new ByteArrayInputStream(result.data()),
                    result.data().length,
                    result.contentType()
            );

            return new ReportStoredFile(
                    storageType,
                    fileKey,
                    result.fileName(),
                    result.contentType(),
                    (long) result.data().length
            );
        } catch (Exception e) {
            throw new RuntimeException("個人別帳票ファイル保存に失敗しました。", e);
        }
    }

    public byte[] load(
            StorageType storageType,
            String fileKey
    ) {
        validator.validateLoadRequest(storageType, fileKey);

        try (var inputStream = storageService.load(storageType, fileKey)) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("帳票ファイル読込に失敗しました。 fileKey=" + fileKey, e);
        }
    }

    public boolean exists(
            StorageType storageType,
            String fileKey
    ) {
        if (storageType == null || !StringUtils.hasText(fileKey)) {
            return false;
        }

        return storageService.exists(storageType, fileKey);
    }

    public void delete(
            StorageType storageType,
            String fileKey
    ) {
        if (storageType == null || !StringUtils.hasText(fileKey)) {
            return;
        }

        storageService.delete(storageType, fileKey);
    }
}
