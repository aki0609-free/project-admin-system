package com.project.backend.features.admin.document.dto;

import java.time.Instant;

public record SyncfusionFileManagerContent(
        String name,
        long size,
        Instant dateModified,
        Instant dateCreated,
        boolean hasChild,
        boolean isFile,
        String type,
        String filterPath,
        String id,
        SyncfusionFileManagerPermission permission
) {
}
