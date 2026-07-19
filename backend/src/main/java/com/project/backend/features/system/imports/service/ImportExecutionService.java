package com.project.backend.features.system.imports.service;

import java.nio.file.Path;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.features.system.imports.dto.ImportExecuteResult;
import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.enums.ImportSourceType;
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

    public ImportExecuteResult executeUpload(
            String targetCode,
            MultipartFile file
    ) {
        try {
            ImportTargetDefinition target =
                    importTargetAdminService.findByTargetCode(targetCode);

            if (target.sourceType() != ImportSourceType.UPLOAD) {
                throw new RuntimeException("この定義はUPLOAD取込ではありません。 targetCode=" + targetCode);
            }

            Path tempFile = uploadFileService.saveToTempFile(file);

            String originalFileName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "upload.csv";

            return csvJobLauncherService.run(
                    target,
                    tempFile,
                    originalFileName
            );

        } catch (Exception e) {
            throw new RuntimeException("CSVインポートに失敗しました。", e);
        }
    }

    public ImportExecuteResult executeFromDefinition(String targetCode) {
        try {
            ImportTargetDefinition target =
                    importTargetAdminService.findByTargetCode(targetCode);

            if (target.sourceType() == ImportSourceType.UPLOAD) {
                throw new RuntimeException("この定義はUPLOAD取込です。 targetCode=" + targetCode);
            }

            if (target.sourceType() == ImportSourceType.SCRIPT) {
                scriptExecutorService.execute(target);
            }

            Path csvPath = csvPathResolver.resolveExisting(
                    target.fixedFilePath()
            );

            return csvJobLauncherService.run(
                    target,
                    csvPath,
                    csvPath.getFileName().toString()
            );

        } catch (Exception e) {
            throw new RuntimeException("定義ベースのCSVインポートに失敗しました。", e);
        }
    }
}