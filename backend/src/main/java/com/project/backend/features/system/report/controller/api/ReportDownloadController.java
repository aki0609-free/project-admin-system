package com.project.backend.features.system.report.controller.api;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.report.dto.ReportDownloadFile;
import com.project.backend.features.system.report.service.core.ReportDownloadService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/report-histories")
@RequiredArgsConstructor
public class ReportDownloadController {

    private final ReportDownloadService reportDownloadService;

    @SuppressWarnings("null")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadHistoryFile(@PathVariable Long id) {
        ReportDownloadFile file = reportDownloadService.downloadHistoryFile(id);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDispositionInline(file.fileName())
                )
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .body(file.data());
    }

    private String contentDispositionInline(String fileName) {
        String encoded = java.net.URLEncoder.encode(
                fileName,
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        return "inline; filename=\"" + sanitizeAsciiFileName(fileName) + "\"; filename*=UTF-8''" + encoded;
    }

    private String sanitizeAsciiFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "report.pdf";
        }

        return fileName.replaceAll("[\\\\/\\r\\n\"]", "_");
    }
}