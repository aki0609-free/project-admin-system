package com.project.backend.features.system.backup.service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.backup.dto.SingleBackupFile;

@Service
public class BackupZipService {

    public byte[] createZip(List<SingleBackupFile> files) {
        if (CollectionUtils.isEmpty(files)) {
            throw new RuntimeException("ZIP対象ファイルがありません。");
        }

        try (
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ZipOutputStream zipOut = new ZipOutputStream(out)
        ) {
            for (SingleBackupFile file : files) {
                validateFile(file);

                ZipEntry entry = new ZipEntry(safeFileName(file.fileName()));

                zipOut.putNextEntry(entry);
                zipOut.write(file.data());
                zipOut.closeEntry();
            }

            zipOut.finish();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("ZIP生成に失敗しました。", e);
        }
    }

    private void validateFile(SingleBackupFile file) {
        if (file == null) {
            throw new RuntimeException("ZIP対象ファイルが不正です。");
        }

        if (!StringUtils.hasText(file.fileName())) {
            throw new RuntimeException("ZIP対象ファイル名が未設定です。");
        }

        if (file.data() == null) {
            throw new RuntimeException("ZIP対象データが未設定です。 fileName=" + file.fileName());
        }
    }

    private String safeFileName(String fileName) {
        return fileName
                .replace("\\", "_")
                .replace("/", "_")
                .replace("..", "_")
                .trim();
    }
}