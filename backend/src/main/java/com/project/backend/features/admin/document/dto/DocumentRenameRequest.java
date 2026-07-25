package com.project.backend.features.admin.document.dto;

public record DocumentRenameRequest(
        String path,
        String newName,
        boolean directory
) {
}
