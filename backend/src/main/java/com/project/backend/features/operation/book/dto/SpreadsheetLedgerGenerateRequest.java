package com.project.backend.features.operation.book.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SpreadsheetLedgerGenerateRequest(
        @NotBlank
        @Pattern(
                regexp = "\\d{4}-(0[1-9]|1[0-2])",
                message = "targetMonth はYYYY-MM形式で指定してください。"
        )
        String targetMonth,
        List<String> selectionValues
) {
    public SpreadsheetLedgerGenerateRequest {
        selectionValues = selectionValues == null
                ? List.of()
                : List.copyOf(selectionValues);
    }

    public SpreadsheetLedgerGenerateRequest(String targetMonth) {
        this(targetMonth, List.of());
    }
}
