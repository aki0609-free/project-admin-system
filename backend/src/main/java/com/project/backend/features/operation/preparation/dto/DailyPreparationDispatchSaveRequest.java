package com.project.backend.features.operation.preparation.dto;

import java.math.BigDecimal;

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

    private Integer distanceFromCompanyKm;

    private Integer vehicleCount = 0;

    private BigDecimal otherAmount = BigDecimal.ZERO;

    private String note;
}
