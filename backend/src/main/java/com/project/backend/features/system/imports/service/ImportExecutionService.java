package com.project.backend.features.system.imports.service;

import java.nio.file.Path;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.features.system.imports.dto.ImportExecuteResult;
import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.enums.ImportSourceType;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.service.resolver.ImportCsvPathResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportExecutionService {

    private final ImportTargetAdminService importTargetAdminService;
    private final ImportUploadFileService uploadFileService;
    private final ImportScriptExecutorService scriptExecutorService;
    private final ImportCsvJobLauncherService csvJobLauncherService;
    private final ImportCsvPathResolver csvPathResolver;
    private final ImportHistoryService importHistoryService;

    public ImportExecuteResult executeUpload(
            String targetCode,
            MultipartFile file
    ) {
        Path tempFile = null;
        ImportTargetDefinition target = null;
        String fileName = file != null ? file.getOriginalFilename() : null;

        try {
            target = importTargetAdminService.findByTargetCode(targetCode);

            if (target.sourceType() != ImportSourceType.UPLOAD) {
                throw new RuntimeException("この定義はUPLOAD取込ではありません。 targetCode=" + targetCode);
            }

            tempFile = uploadFileService.saveToTempFile(file);

            fileName = tempFile.getFileName().toString();

            Path importPath = tempFile;
            if (target.scriptType() != null
                    && target.scriptType() != ImportScriptType.NONE) {
                scriptExecutorService.execute(target, tempFile);
                importPath = csvPathResolver.resolveExisting(
                        target.fixedFilePath()
                );
            }

            return csvJobLauncherService.run(
                    target,
                    importPath,
                    fileName
            );

        } catch (Exception e) {
            savePreJobFailure(target, fileName, e);
            throw new RuntimeException(
                    "CSVインポートに失敗しました。 "
                            + rootMessage(e),
                    e
            );
        } finally {
            uploadFileService.deleteTempFile(tempFile);
        }
    }

    public ImportExecuteResult executeFromDefinition(String targetCode) {
        ImportTargetDefinition target = null;
        String fileName = null;

        try {
            target = importTargetAdminService.findByTargetCode(targetCode);

            if (target.sourceType() == ImportSourceType.UPLOAD) {
                throw new RuntimeException("この定義はUPLOAD取込です。 targetCode=" + targetCode);
            }

            if (target.sourceType() == ImportSourceType.SCRIPT) {
                scriptExecutorService.execute(target);
            }

            Path csvPath = csvPathResolver.resolveExisting(
                    target.fixedFilePath()
            );
            fileName = csvPath.getFileName().toString();

            return csvJobLauncherService.run(
                    target,
                    csvPath,
                    fileName
            );

        } catch (Exception e) {
            savePreJobFailure(target, fileName, e);
            throw new RuntimeException(
                    "定義ベースのCSVインポートに失敗しました。 "
                            + rootMessage(e),
                    e
            );
        }
    }

    private void savePreJobFailure(
            ImportTargetDefinition target,
            String fileName,
            Exception exception
    ) {
        if (target == null) {
            return;
        }

        importHistoryService.saveFailure(
                target,
                fileName,
                null,
                currentUsername(),
                new RuntimeException(rootMessage(exception), exception)
        );
    }

    private String currentUsername() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return "system";
        }

        return authentication.getName();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = null;

        while (current != null) {
            if (current.getMessage() != null
                    && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
            current = current.getCause();
        }

        return message != null ? message : throwable.getClass().getSimpleName();
    }
}
