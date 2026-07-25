package com.project.backend.features.admin.document.dto;

import java.time.Instant;

public record DocumentEntryResponse(
        String path,
        String name,
        boolean directory,
        long size,
        Instant lastModified,
        String eTag
) {
}
