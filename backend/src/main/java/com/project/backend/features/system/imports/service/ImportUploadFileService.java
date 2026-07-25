package com.project.backend.features.system.imports.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportUploadFileService {

    private static final long MAX_UPLOAD_BYTES =
            20L * 1024L * 1024L;

    @SuppressWarnings("null")
    public Path saveToTempFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("CSVファイルを選択してください。");
        }

        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException(
                    "アップロードできるCSVは20MBまでです。"
            );
        }

        try {
            Path tempDir = Files.createTempDirectory("import-csv-");
            String originalFileName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "upload.csv";

            String safeFileName = Path.of(originalFileName)
                    .getFileName()
                    .toString();

            if (!safeFileName.toLowerCase().endsWith(".csv")) {
                throw new IllegalArgumentException(
                        "CSVファイルだけアップロードできます。"
                );
            }

            Path tempFile = tempDir.resolve(safeFileName).normalize();

            if (!tempFile.startsWith(tempDir)) {
                throw new IllegalArgumentException(
                        "アップロードファイル名が不正です。"
                );
            }

            file.transferTo(tempFile.toFile());
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("アップロードCSVの一時保存に失敗しました。", e);
        }
    }

    public void deleteTempFile(Path tempFile) {
        if (tempFile == null || tempFile.getParent() == null) {
            return;
        }

        try (var paths = Files.walk(tempFile.getParent())) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                            // 一時ファイル削除失敗で取込結果を失敗にしない。
                        }
                    });
        } catch (Exception ignored) {
            // 一時ディレクトリが存在しない場合を含めて無視する。
        }
    }
}
