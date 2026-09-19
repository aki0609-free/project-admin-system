package com.project.backend.features.operation.preparation.dto;

import com.project.backend.features.dailyreport.enums.VehicleArrangementType;

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

    private VehicleArrangementType vehicleArrangementType;

    private Integer passengerCount;

    private String workDescription;
}
