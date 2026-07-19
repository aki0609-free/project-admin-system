package com.project.backend.features.system.backup.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.backup.dto.BackupExecuteRequest;
import com.project.backend.features.system.backup.dto.BackupExecutionResult;
import com.project.backend.features.system.backup.dto.BackupHistoryFileDownloadResult;
import com.project.backend.features.system.backup.dto.BackupHistoryResponse;
import com.project.backend.features.system.backup.dto.BackupTargetResponse;
import com.project.backend.features.system.backup.dto.BackupTargetSaveRequest;
import com.project.backend.features.system.backup.service.BackupExecutionService;
import com.project.backend.features.system.backup.service.BackupHistoryFileDownloadService;
import com.project.backend.features.system.backup.service.BackupHistoryService;
import com.project.backend.features.system.backup.service.BackupTargetCommandService;
import com.project.backend.features.system.backup.service.BackupTargetQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupTargetQueryService targetQueryService;
    private final BackupTargetCommandService targetCommandService;
    private final BackupExecutionService backupExecutionService;
    private final BackupHistoryService backupHistoryService;
    private final BackupHistoryFileDownloadService historyFileDownloadService;

    @GetMapping("/targets")
    public List<BackupTargetResponse> findTargets() {
        return targetQueryService.findAll();
    }

    @GetMapping("/targets/{id}")
    public BackupTargetResponse findDetail(
            @PathVariable Long id
    ) {
        return targetQueryService.findDetail(id);
    }

    @PostMapping("/targets")
    public BackupTargetResponse create(
            @RequestBody BackupTargetSaveRequest request
    ) {
        return targetCommandService.create(request);
    }

    @PutMapping("/targets/{id}")
    public BackupTargetResponse update(
            @PathVariable Long id,
            @RequestBody BackupTargetSaveRequest request
    ) {
        return targetCommandService.update(
                id,
                request
        );
    }

    @DeleteMapping("/targets/{id}")
    public void delete(
            @PathVariable Long id
    ) {
        targetCommandService.delete(id);
    }

    @PostMapping("/execute")
    public ResponseEntity<byte[]> execute(
            @RequestBody BackupExecuteRequest request
    ) {
        BackupExecutionResult result =
                backupExecutionService.execute(
                        request != null
                                ? request.targetCodes()
                                : null
                );

        return toDownloadResponse(
                result.fileName(),
                result.contentType(),
                result.data()
        );
    }

    @GetMapping("/histories")
    public List<BackupHistoryResponse> findHistories() {
        return backupHistoryService.findAll();
    }

    @GetMapping("/histories/{historyId}/file")
    public ResponseEntity<byte[]> downloadHistoryFile(
            @PathVariable Long historyId
    ) {
        BackupHistoryFileDownloadResult result =
                historyFileDownloadService.download(historyId);

        return toDownloadResponse(
                result.fileName(),
                result.contentType(),
                result.data()
        );
    }

    private ResponseEntity<byte[]> toDownloadResponse(
            String fileName,
            String contentType,
            byte[] data
    ) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(
                                fileName,
                                StandardCharsets.UTF_8
                        )
                        .build()
        );

        headers.add(
                HttpHeaders.CONTENT_TYPE,
                contentType
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}