package com.project.backend.features.operation.book.dto;

import java.util.Map;

public record SpreadsheetLedgerSelectionOptionResponse(
        String value,
        Map<String, Object> displayValues
) {
    public SpreadsheetLedgerSelectionOptionResponse {
        displayValues = java.util.Collections.unmodifiableMap(
                new java.util.LinkedHashMap<>(displayValues)
        );
    }
}
