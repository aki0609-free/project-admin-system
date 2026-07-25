package com.project.backend.features.admin.document.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.backend.features.admin.document.dto.DocumentAreaResponse;
import com.project.backend.features.admin.document.dto.DocumentCopyMoveRequest;
import com.project.backend.features.admin.document.dto.DocumentDirectoryCreateRequest;
import com.project.backend.features.admin.document.dto.DocumentDownload;
import com.project.backend.features.admin.document.dto.DocumentEntryResponse;
import com.project.backend.features.admin.document.dto.DocumentListResponse;
import com.project.backend.features.admin.document.dto.DocumentRenameRequest;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.service.DocumentManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/documents")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class DocumentManagementController {

    private final DocumentManagementService service;

    @GetMapping("/areas")
    public List<DocumentAreaResponse> findAreas() {
        return service.findAreas();
    }

    @GetMapping("/{area}/entries")
    public DocumentListResponse list(
            @PathVariable DocumentArea area,
            @RequestParam(defaultValue = "") String path,
            @RequestParam(required = false) String continuationToken,
            @RequestParam(defaultValue = "100") int maxKeys
    ) {
        return service.list(
                area,
                path,
                continuationToken,
                maxKeys
        );
    }

    @GetMapping("/{area}/search")
    public List<DocumentEntryResponse> search(
            @PathVariable DocumentArea area,
            @RequestParam String keyword
    ) {
        return service.search(area, keyword);
    }

    @GetMapping("/{area}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable DocumentArea area,
            @RequestParam String path
    ) {
        DocumentDownload download = service.download(area, path);
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
                .body(new InputStreamResource(
                        download.inputStream()
                ));
    }

    @PostMapping("/{area}/directories")
    public ResponseEntity<Void> createDirectory(
            @PathVariable DocumentArea area,
            @RequestBody DocumentDirectoryCreateRequest request
    ) {
        service.createDirectory(area, request.path());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/{area}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> upload(
            @PathVariable DocumentArea area,
            @RequestParam(defaultValue = "") String path,
            @RequestPart MultipartFile file
    ) {
        service.upload(area, path, file);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{area}/copy")
    public ResponseEntity<Void> copy(
            @PathVariable DocumentArea area,
            @RequestBody DocumentCopyMoveRequest request
    ) {
        service.copy(
                area,
                request.sourcePath(),
                request.targetPath(),
                request.directory()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{area}/move")
    public ResponseEntity<Void> move(
            @PathVariable DocumentArea area,
            @RequestBody DocumentCopyMoveRequest request
    ) {
        service.move(
                area,
                request.sourcePath(),
                request.targetPath(),
                request.directory()
        );
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{area}/rename")
    public ResponseEntity<Void> rename(
            @PathVariable DocumentArea area,
            @RequestBody DocumentRenameRequest request
    ) {
        service.rename(
                area,
                request.path(),
                request.newName(),
                request.directory()
        );
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{area}")
    public ResponseEntity<Void> delete(
            @PathVariable DocumentArea area,
            @RequestParam String path,
            @RequestParam boolean directory
    ) {
        service.delete(area, path, directory);
        return ResponseEntity.noContent().build();
    }
}
