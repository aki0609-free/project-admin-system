package com.project.backend.features.system.imports.dto;

import lombok.Builder;

@Builder
public record ImportScriptFileResponse(
        String fileName,
        String filePath,
        String extension
) {
}