package com.project.backend.batch.dto;

import java.util.Map;

public record CsvImportRow(
        long rowNo,
        Map<String, String> values
) {
}