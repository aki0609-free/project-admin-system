package com.project.backend.features.system.backup.service;

import java.io.ByteArrayInputStream;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.backup.dto.BackupStoredFile;
import com.project.backend.features.system.backup.service.builder.BackupFileKeyBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupFileStorageService {

    private final StorageService storageService;
    private final BackupFileKeyBuilder fileKeyBuilder;

    public BackupStoredFile save(
            String outputDir,
            String fileName,
            String contentType,
            byte[] data
    ) {
        if (!StringUtils.hasText(fileName)) {
            throw new RuntimeException("バックアップファイル名が未設定です。");
        }

        if (data == null) {
            throw new RuntimeException("バックアップデータが未設定です。");
        }

        String fileKey = fileKeyBuilder.build(
                outputDir,
                fileName
        );

        storageService.save(
                fileKey,
                new ByteArrayInputStream(data),
                data.length,
                contentType
        );

        return new BackupStoredFile(
                storageService.type(),
                fileKey,
                fileName,
                contentType,
                (long) data.length
        );
    }
}