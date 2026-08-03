package com.project.backend.features.operation.book.dto;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;

public record SpreadsheetLedgerSaveRequest(
        @NotNull JsonNode workbook
) {
}
