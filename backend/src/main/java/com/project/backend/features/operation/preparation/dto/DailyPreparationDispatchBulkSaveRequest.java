package com.project.backend.features.operation.preparation.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyPreparationDispatchBulkSaveRequest {

    @NotNull
    private Long preparationId;

    private List<DailyPreparationDispatchBulkSaveItemRequest> items = List.of();
}