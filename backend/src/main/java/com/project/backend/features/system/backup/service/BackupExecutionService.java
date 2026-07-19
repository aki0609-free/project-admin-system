package com.project.backend.features.system.backup.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.backup.dto.BackupExecutionResult;
import com.project.backend.features.system.backup.dto.BackupRequest;
import com.project.backend.features.system.backup.dto.SingleBackupFile;
import com.project.backend.features.system.backup.service.builder.BackupExecutionResultBuilder;
import com.project.backend.features.system.backup.service.builder.BackupSingleFileBuilder;
import com.project.backend.features.system.backup.service.resolver.BackupOutputResolver;
import com.project.backend.features.system.backup.service.validation.BackupExecutionValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupExecutionService {

    private final BackupSingleFileBuilder singleFileBuilder;
    private final BackupExecutionResultBuilder resultBuilder;
    private final BackupHistoryService historyService;
    private final BackupExecutionValidator validator;
    private final BackupOutputResolver outputResolver;

    @Transactional
    public BackupExecutionResult execute(List<String> targetCodes) {
        return execute(
                BackupRequest.builder()
                        .targetCodes(targetCodes)
                        .encoding("UTF-8")
                        .zipOutput(null)
                        .build()
        );
    }

    @Transactional
    public BackupExecutionResult execute(BackupRequest request) {
        validator.validate(request);

        try {
            List<SingleBackupFile> files = buildFiles(request);

            BackupExecutionResult result = buildResult(
                    request,
                    files
            );

            historyService.saveSuccess(
                    request,
                    result
            );

            return result;

        } catch (Exception e) {
            historyService.saveFailure(
                    request,
                    e
            );

            throw e;
        }
    }

    private List<SingleBackupFile> buildFiles(BackupRequest request) {
        return request.targetCodes()
                .stream()
                .map(singleFileBuilder::build)
                .toList();
    }

    private BackupExecutionResult buildResult(
            BackupRequest request,
            List<SingleBackupFile> files
    ) {
        if (files == null || files.isEmpty()) {
            throw new RuntimeException("バックアップ対象ファイルが生成されませんでした。");
        }

        if (outputResolver.shouldZip(request, files)) {
            return resultBuilder.buildZip(files);
        }

        return resultBuilder.buildSingle(files.get(0));
    }
}