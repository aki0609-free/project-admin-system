package com.project.backend.features.operation.preparation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyPreparationDispatchSaveRequest {

    @NotNull
    private Long preparationId;

    private Long customerId;

    @NotNull
    private Long customerSiteId;

    private Integer vehicleCount = 0;

    private String note;
}