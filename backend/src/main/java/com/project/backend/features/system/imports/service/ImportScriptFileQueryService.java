package com.project.backend.features.system.imports.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.app.storage.model.StorageEntry;
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

        return storageService.listRecursively(prefix)
                .stream()
                .filter(entry -> !entry.directory())
                .map(StorageEntry::key)
                .filter(this::isScriptFile)
                .map(filePath -> toResponse(prefix, filePath))
                .sorted((a, b) -> a.filePath().compareToIgnoreCase(b.filePath()))
                .toList();
    }

    private ImportScriptFileResponse toResponse(
            String prefix,
            String filePath
    ) {
        String normalizedPath = filePath.replace("\\", "/");
        String normalizedPrefix = prefix.replace("\\", "/");
        if (normalizedPath.startsWith(normalizedPrefix + "/")) {
            normalizedPath = normalizedPath.substring(
                    normalizedPrefix.length() + 1
            );
        }

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
