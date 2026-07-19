package com.project.backend.features.system.batch.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.repository.BatchExecutionLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchExecutionFileService {

    private final BatchExecutionLogRepository repository;
    private final StorageService storageService;

    @SuppressWarnings("null")
    public ResponseEntity<ByteArrayResource> download(Long logId) {
        @SuppressWarnings("null")
        BatchExecutionLog log = repository.findById(logId)
                .orElseThrow(() -> new RuntimeException("log not found. id=" + logId));

        validateDownloadable(log);

        byte[] bytes = loadBytes(
                log.getStorageType(),
                log.getOutputFileKey()
        );

        String contentType = Optional.ofNullable(log.getContentType())
                .filter(StringUtils::hasText)
                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        String fileName = Optional.ofNullable(log.getOutputFileName())
                .filter(StringUtils::hasText)
                .orElse("download");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(bytes.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(new ByteArrayResource(bytes));
    }

    private void validateDownloadable(BatchExecutionLog log) {
        if (log.getStorageType() == null) {
            throw new RuntimeException("storageType がありません。 logId=" + log.getId());
        }

        if (!StringUtils.hasText(log.getOutputFileKey())) {
            throw new RuntimeException("出力ファイルキーがありません。 logId=" + log.getId());
        }

        if (!storageService.exists(
                log.getStorageType(),
                log.getOutputFileKey()
        )) {
            throw new RuntimeException(
                    "出力ファイルが存在しません。 logId="
                            + log.getId()
                            + ", storageType="
                            + log.getStorageType()
                            + ", key="
                            + log.getOutputFileKey()
            );
        }
    }

    private byte[] loadBytes(
            StorageType storageType,
            String key
    ) {
        try (InputStream inputStream = storageService.load(storageType, key)) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(
                    "出力ファイルの読込に失敗しました。 storageType="
                            + storageType
                            + ", key="
                            + key,
                    e
            );
        }
    }
}