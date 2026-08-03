package com.project.backend.features.operation.book.dto;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

public record SpreadsheetLedgerGenerateResponse(
        Long masterId,
        String bookCode,
        String bookName,
        String targetMonth,
        int rowCount,
        Instant generatedAt,
        String storagePath,
        int workbookBytes,
        long generationDurationMs,
        boolean editable,
        JsonNode workbook,
        String selectionValue
) {
    public SpreadsheetLedgerGenerateResponse(
            Long masterId,
            String bookCode,
            String bookName,
            String targetMonth,
            int rowCount,
            Instant generatedAt,
            String storagePath,
            int workbookBytes,
            long generationDurationMs,
            boolean editable,
            JsonNode workbook
    ) {
        this(
                masterId,
                bookCode,
                bookName,
                targetMonth,
                rowCount,
                generatedAt,
                storagePath,
                workbookBytes,
                generationDurationMs,
                editable,
                workbook,
                null
        );
    }
}
