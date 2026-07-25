package com.project.backend.features.system.batch.controller;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.batch.dto.BatchExecuteRequest;
import com.project.backend.features.system.batch.dto.BatchExecuteResponse;
import com.project.backend.features.system.batch.dto.BatchExecutionLogResponse;
import com.project.backend.features.system.batch.service.BatchExecutionFileService;
import com.project.backend.features.system.batch.service.BatchExecutionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/batch")
@RequiredArgsConstructor
public class BatchExecutionController {

    private final BatchExecutionService batchExecutionService;
    private final BatchExecutionFileService batchExecutionFileService;

    @PostMapping("/execute/{jobCode}")
    @PreAuthorize("isAuthenticated()")
    public BatchExecuteResponse executeNow(
            @PathVariable String jobCode,
            @RequestBody(required = false) BatchExecuteRequest request
    ) {
        return batchExecutionService.executeNow(
                jobCode,
                request != null && request.params() != null
                        ? request.params()
                        : Map.of()
        );
    }

    @PostMapping("/retry/{logId}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public BatchExecuteResponse retry(@PathVariable Long logId) {
        return batchExecutionService.retry(logId);
    }

    @PostMapping("/scheduled-execute/{jobCode}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public BatchExecuteResponse executeScheduledManually(
            @PathVariable String jobCode
    ) {
        return batchExecutionService.executeScheduled(jobCode);
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public List<BatchExecutionLogResponse> findLogs() {
        return batchExecutionService.findLogs();
    }

    @GetMapping("/logs/{logId}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ByteArrayResource> downloadLogFile(
            @PathVariable Long logId
    ) {
        return batchExecutionFileService.download(logId);
    }

    @GetMapping("/logs/job/{jobCode}")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public List<BatchExecutionLogResponse> findLogsByJobCode(
            @PathVariable String jobCode
    ) {
        return batchExecutionService.findLogsByJobCode(jobCode);
    }
}
