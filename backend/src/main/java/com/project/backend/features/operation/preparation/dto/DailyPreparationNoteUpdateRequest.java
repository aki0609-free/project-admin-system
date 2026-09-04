package com.project.backend.features.operation.preparation.dto;

import jakarta.validation.constraints.Size;

public record DailyPreparationNoteUpdateRequest(
        @Size(max = 1000) String note
) {
}
