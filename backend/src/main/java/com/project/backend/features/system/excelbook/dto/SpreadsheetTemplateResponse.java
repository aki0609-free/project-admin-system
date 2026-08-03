package com.project.backend.features.system.excelbook.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record SpreadsheetTemplateResponse(
        Long masterId,
        String bookCode,
        String storagePath,
        JsonNode workbook
) {
}
