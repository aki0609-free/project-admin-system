package com.project.backend.features.system.backup.service.builder;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.backup.dto.BackupExecutionResult;
import com.project.backend.features.system.backup.dto.BackupStoredFile;
import com.project.backend.features.system.backup.dto.SingleBackupFile;
import com.project.backend.features.system.backup.service.BackupFileStorageService;
import com.project.backend.features.system.backup.service.BackupZipService;
import com.project.backend.features.system.backup.service.resolver.BackupOutputResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BackupExecutionResultBuilder {

    private static final String CONTENT_TYPE_CSV = "text/csv; charset=UTF-8";
    private static final String CONTENT_TYPE_ZIP = "application/zip";

    private final BackupZipService zipService;
    private final BackupFileNameBuilder fileNameBuilder;
    private final BackupFileStorageService fileStorageService;
    private final BackupOutputResolver outputResolver;

    public BackupExecutionResult buildSingle(SingleBackupFile file) {
        BackupStoredFile storedFile = saveSingleFileIfNeeded(file);

        return BackupExecutionResult.builder()
                .fileName(file.fileName())
                .contentType(CONTENT_TYPE_CSV)
                .data(file.data())
                .zipOutput(false)
                .storedFile(storedFile)
                .build();
    }

    public BackupExecutionResult buildZip(List<SingleBackupFile> files) {
        byte[] zipData = zipService.createZip(files);
        String zipFileName = fileNameBuilder.buildZipFileName();

        BackupStoredFile storedFile = saveZipIfNeeded(
                files,
                zipFileName,
                zipData
        );

        return BackupExecutionResult.builder()
                .fileName(zipFileName)
                .contentType(CONTENT_TYPE_ZIP)
                .data(zipData)
                .zipOutput(true)
                .storedFile(storedFile)
                .build();
    }

    private BackupStoredFile saveSingleFileIfNeeded(SingleBackupFile file) {
        if (!outputResolver.shouldSaveToServer(file.outputMode())) {
            return null;
        }

        return fileStorageService.save(
                file.outputDir(),
                file.fileName(),
                CONTENT_TYPE_CSV,
                file.data()
        );
    }

    private BackupStoredFile saveZipIfNeeded(
            List<SingleBackupFile> files,
            String zipFileName,
            byte[] zipData
    ) {
        List<SingleBackupFile> serverFileTargets = files.stream()
                .filter(file -> outputResolver.shouldSaveToServer(file.outputMode()))
                .toList();

        if (serverFileTargets.isEmpty()) {
            return null;
        }

        String outputDir = outputResolver.resolveZipOutputDir(serverFileTargets);

        return fileStorageService.save(
                outputDir,
                zipFileName,
                CONTENT_TYPE_ZIP,
                zipData
        );
    }
}