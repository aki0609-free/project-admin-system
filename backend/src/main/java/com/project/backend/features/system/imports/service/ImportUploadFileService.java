package com.project.backend.features.system.imports.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportUploadFileService {

    @SuppressWarnings("null")
    public Path saveToTempFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("CSVファイルを選択してください。");
        }

        try {
            Path tempDir = Files.createTempDirectory("import-csv-");
            String originalFileName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "upload.csv";
            Path tempFile = tempDir.resolve(originalFileName);
            file.transferTo(tempFile.toFile());
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("アップロードCSVの一時保存に失敗しました。", e);
        }
    }
}