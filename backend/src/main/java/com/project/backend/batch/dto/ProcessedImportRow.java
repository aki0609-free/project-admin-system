package com.project.backend.batch.dto;

import java.util.Map;

public record ProcessedImportRow(
        long rowNo,
        Map<String, Object> values
) {
}