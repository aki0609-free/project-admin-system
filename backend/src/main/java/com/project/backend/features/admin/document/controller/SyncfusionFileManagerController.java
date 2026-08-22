package com.project.backend.features.admin.document.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.project.backend.features.admin.document.dto.SyncfusionFileManagerDownload;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerRequest;
import com.project.backend.features.admin.document.dto.SyncfusionFileManagerResponse;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.service.DocumentManagementService;
import com.project.backend.features.admin.document.service.SyncfusionFileManagerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/documents/file-manager")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class SyncfusionFileManagerController {

    private final SyncfusionFileManagerService fileManagerService;
    private final DocumentManagementService documentService;

    @PostMapping("/{area}/operations")
    public SyncfusionFileManagerResponse operations(
            @PathVariable DocumentArea area,
            @RequestBody SyncfusionFileManagerRequest request
    ) {
        return fileManagerService.execute(area, request);
    }

    @PostMapping(
            value = "/{area}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> upload(
            @PathVariable DocumentArea area,
            @RequestParam(defaultValue = "/") String path,
            @RequestPart("uploadFiles") List<MultipartFile> files
    ) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "アップロードファイルは必須です。"
            );
        }
        String relativePath = toRelativePath(path);
        files.forEach(file ->
                documentService.upload(area, relativePath, file));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/{area}/download",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable DocumentArea area,
            @RequestBody SyncfusionFileManagerRequest request
    ) {
        SyncfusionFileManagerDownload download =
                fileManagerService.download(area, request);
        String encodedFileName = URLEncoder.encode(
                download.fileName(),
                StandardCharsets.UTF_8
        ).replace("+", "%20");

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''"
                                + encodedFileName
                )
                .contentType(MediaType.parseMediaType(
                        download.contentType()
                ))
                .body(download.body());
    }

    private String toRelativePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
            return "";
        }

        String value = path.trim().replace("\\", "/");
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
