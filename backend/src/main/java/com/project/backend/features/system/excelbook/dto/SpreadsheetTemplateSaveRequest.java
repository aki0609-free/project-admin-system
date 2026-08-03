package com.project.backend.features.system.excelbook.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record SpreadsheetTemplateSaveRequest(
        JsonNode workbook
) {
}
