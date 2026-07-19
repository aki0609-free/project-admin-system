package com.project.backend.features.system.backup.service.builder;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.backup.dto.BackupExecutionResult;
import com.project.backend.features.system.backup.dto.BackupRequest;
import com.project.backend.features.system.backup.dto.BackupStoredFile;
import com.project.backend.features.system.backup.entity.BackupHistory;
import com.project.backend.features.system.backup.enums.BackupHistoryStatus;

@Component
public class BackupHistoryBuilder {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 4000;
    private static final String DEFAULT_EXECUTED_BY = "system";

    public BackupHistory buildSuccess(
            BackupRequest request,
            BackupExecutionResult result
    ) {
        BackupHistory history = new BackupHistory();
        BackupStoredFile storedFile = result.storedFile();

        history.setTargetCodes(joinTargetCodes(request));
        history.setFileName(result.fileName());
        history.setContentType(result.contentType());
        history.setFileSize(result.data() != null ? (long) result.data().length : null);
        history.setZipOutput(result.zipOutput());

        if (storedFile != null) {
            history.setStorageType(storedFile.storageType());
            history.setStoredFileKey(storedFile.fileKey());
            history.setStoredFileName(storedFile.fileName());
            history.setContentType(storedFile.contentType());
            history.setFileSize(storedFile.fileSize());
        }

        history.setStatus(BackupHistoryStatus.SUCCESS);
        history.setExecutedBy(DEFAULT_EXECUTED_BY);
        history.setExecutedAt(Instant.now());
        history.setErrorMessage(null);

        return history;
    }

    public BackupHistory buildFailure(
            BackupRequest request,
            Exception exception
    ) {
        BackupHistory history = new BackupHistory();

        history.setTargetCodes(joinTargetCodes(request));
        history.setStatus(BackupHistoryStatus.FAILED);
        history.setExecutedBy(DEFAULT_EXECUTED_BY);
        history.setExecutedAt(Instant.now());
        history.setErrorMessage(limit(
                exception != null ? exception.getMessage() : null,
                ERROR_MESSAGE_MAX_LENGTH
        ));

        return history;
    }

    private String joinTargetCodes(BackupRequest request) {
        if (request == null || request.targetCodes() == null) {
            return "";
        }

        return String.join(",", request.targetCodes());
    }

    private String limit(
            String value,
            int maxLength
    ) {
        if (value == null) {
            return null;
        }

        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}