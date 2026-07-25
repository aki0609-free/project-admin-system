package com.project.backend.features.admin.document.dto;

public record DocumentCopyMoveRequest(
        String sourcePath,
        String targetPath,
        boolean directory
) {
}
