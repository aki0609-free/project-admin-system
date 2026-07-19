package com.project.backend.features.system.backup.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.backup.dto.BackupHistoryFileDownloadResult;
import com.project.backend.features.system.backup.entity.BackupHistory;
import com.project.backend.features.system.backup.enums.BackupHistoryStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupHistoryFileDownloadService {

    private final BackupHistoryLookupService lookupService;
    private final StorageService storageService;

    public BackupHistoryFileDownloadResult download(Long historyId) {
        BackupHistory history = lookupService.find(historyId);

        validate(history);

        try (var inputStream = storageService.load(
                history.getStorageType(),
                history.getStoredFileKey()
        )) {
            return BackupHistoryFileDownloadResult.builder()
                    .fileName(resolveFileName(history))
                    .contentType(resolveContentType(history))
                    .data(inputStream.readAllBytes())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(
                    "バックアップファイルの読込に失敗しました。 historyId=" + historyId,
                    e
            );
        }
    }

    private void validate(BackupHistory history) {
        if (history.getStatus() != BackupHistoryStatus.SUCCESS) {
            throw new RuntimeException("成功したバックアップ履歴のみダウンロードできます。");
        }

        if (history.getStorageType() == null) {
            throw new RuntimeException("保存先StorageTypeが記録されていません。");
        }

        if (!StringUtils.hasText(history.getStoredFileKey())) {
            throw new RuntimeException("保存済みファイルキーが記録されていません。");
        }

        if (!storageService.exists(
                history.getStorageType(),
                history.getStoredFileKey()
        )) {
            throw new RuntimeException(
                    "保存済みバックアップファイルが存在しません。 key="
                            + history.getStoredFileKey()
            );
        }
    }

    private String resolveFileName(BackupHistory history) {
        if (StringUtils.hasText(history.getStoredFileName())) {
            return history.getStoredFileName();
        }

        if (StringUtils.hasText(history.getFileName())) {
            return history.getFileName();
        }

        return "backup.dat";
    }

    private String resolveContentType(BackupHistory history) {
        if (StringUtils.hasText(history.getContentType())) {
            return history.getContentType();
        }

        return "application/octet-stream";
    }
}