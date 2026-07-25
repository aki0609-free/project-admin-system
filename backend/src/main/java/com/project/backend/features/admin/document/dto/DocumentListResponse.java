package com.project.backend.features.admin.document.dto;

import java.util.List;

public record DocumentListResponse(
        List<DocumentEntryResponse> entries,
        String nextContinuationToken,
        boolean truncated
) {

    public DocumentListResponse {
        entries = List.copyOf(entries);
    }
}
