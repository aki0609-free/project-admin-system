package com.project.backend.features.admin.document.dto;

public record SyncfusionFileManagerPermission(
        boolean read,
        boolean write,
        boolean copy,
        boolean download,
        boolean writeContents,
        boolean upload,
        String message
) {
}
