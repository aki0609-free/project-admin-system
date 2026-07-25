package com.project.backend.app.storage.model;

import java.util.List;

public record StorageListPage(
        List<StorageEntry> entries,
        String nextContinuationToken,
        boolean truncated
) {
    public StorageListPage {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
