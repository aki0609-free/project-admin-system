package com.project.backend.features.operation.preparation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyPreparationAssignmentSaveRequest {

    @NotNull
    private Long preparationId;

    @NotNull
    private Long employeeId;

    private Long customerId;

    private Long customerSiteId;

    private String workDescription;
}