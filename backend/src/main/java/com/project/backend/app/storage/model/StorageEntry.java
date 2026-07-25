package com.project.backend.app.storage.model;

import java.time.Instant;

public record StorageEntry(
        String key,
        String name,
        boolean directory,
        long size,
        Instant lastModified,
        String eTag
) {
}
