package com.project.backend.features.admin.document.dto;

public record SyncfusionFileManagerDetails(
        String name,
        String location,
        String size,
        String created,
        String modified,
        boolean multipleFiles,
        Boolean isFile,
        SyncfusionFileManagerPermission permission
) {
}
