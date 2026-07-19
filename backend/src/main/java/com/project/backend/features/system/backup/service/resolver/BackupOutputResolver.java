package com.project.backend.features.system.backup.service.resolver;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.backup.dto.BackupRequest;
import com.project.backend.features.system.backup.dto.SingleBackupFile;
import com.project.backend.features.system.backup.enums.BackupOutputMode;

@Service
public class BackupOutputResolver {

    public boolean shouldZip(
            BackupRequest request,
            List<SingleBackupFile> files
    ) {
        if (Boolean.TRUE.equals(request.zipOutput())) {
            return true;
        }

        if (Boolean.FALSE.equals(request.zipOutput())) {
            return files.size() > 1;
        }

        return files.size() > 1
                || files.stream()
                .anyMatch(SingleBackupFile::zipRequired);
    }

    public boolean shouldSaveToServer(
            BackupOutputMode outputMode
    ) {
        return outputMode == BackupOutputMode.SERVER_FILE
                || outputMode == BackupOutputMode.BOTH;
    }

    public String resolveZipOutputDir(
            List<SingleBackupFile> files
    ) {
        return files.stream()
                .map(SingleBackupFile::outputDir)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElseThrow(
                        () -> new RuntimeException(
                                "ZIP保存先 outputDir が設定されていません。"
                        )
                );
    }
}