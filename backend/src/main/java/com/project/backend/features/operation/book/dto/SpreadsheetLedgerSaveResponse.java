package com.project.backend.features.operation.book.dto;

import java.time.Instant;

public record SpreadsheetLedgerSaveResponse(
        String storagePath,
        int workbookBytes,
        Instant savedAt
) {
}
