package com.project.backend.features.operation.preparation.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyPreparationCreateRequest {

    @NotNull
    private LocalDate targetDate;

    private String note;
}