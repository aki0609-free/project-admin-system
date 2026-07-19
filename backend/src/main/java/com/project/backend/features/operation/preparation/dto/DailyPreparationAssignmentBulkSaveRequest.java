package com.project.backend.features.operation.preparation.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyPreparationAssignmentBulkSaveRequest {

    @NotNull
    private Long preparationId;

    private List<DailyPreparationAssignmentBulkSaveItemRequest> items = List.of();
}