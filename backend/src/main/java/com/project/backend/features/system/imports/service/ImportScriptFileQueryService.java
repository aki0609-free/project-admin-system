package com.project.backend.features.system.imports.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.imports.dto.ImportScriptFileResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportScriptFileQueryService {

    private final StorageService storageService;
    private final StorageProperties storageProperties;

    public List<ImportScriptFileResponse> findAll() {
        String prefix = storageProperties.getImports()
                .getScript()
                .getPath();

        return storageService.list(prefix)
                .stream()
                .filter(this::isScriptFile)
                .map(this::toResponse)
                .sorted((a, b) -> a.filePath().compareToIgnoreCase(b.filePath()))
                .toList();
    }

    private ImportScriptFileResponse toResponse(String filePath) {
        String normalizedPath = filePath.replace("\\", "/");

        return ImportScriptFileResponse.builder()
                .fileName(fileName(normalizedPath))
                .filePath(normalizedPath)
                .extension(extension(normalizedPath))
                .build();
    }

    private boolean isScriptFile(String filePath) {
        String extension = extension(filePath);

        return extension.equals("py")
                || extension.equals("sh");
    }

    private String fileName(String filePath) {
        int index = filePath.lastIndexOf("/");

        if (index < 0) {
            return filePath;
        }

        return filePath.substring(index + 1);
    }

    private String extension(String filePath) {
        int index = filePath.lastIndexOf(".");

        if (index < 0 || index == filePath.length() - 1) {
            return "";
        }

        return filePath.substring(index + 1).toLowerCase();
    }
}