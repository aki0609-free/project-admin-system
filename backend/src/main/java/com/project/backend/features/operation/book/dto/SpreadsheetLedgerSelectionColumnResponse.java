package com.project.backend.features.operation.book.dto;

public record SpreadsheetLedgerSelectionColumnResponse(
        String columnName,
        String displayName,
        String dataType,
        Integer orderNo
) {
}
