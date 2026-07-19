package com.project.backend.features.operation.preparation.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.operation.preparation.dto.*;
import com.project.backend.features.operation.preparation.entity.*;

@Component
public class DailyPreparationMapper {

    public DailyPreparationResponse toResponse(
            DailyPreparation entity,
            List<DailyPreparationAssignment> assignments,
            List<DailyPreparationDispatch> dispatches
    ) {
        return DailyPreparationResponse.builder()
                .id(entity.getId())
                .targetDate(entity.getTargetDate())
                .status(entity.getStatus())
                .note(entity.getNote())
                .assignments(assignments.stream().map(this::toAssignmentResponse).toList())
                .dispatches(dispatches.stream().map(this::toDispatchResponse).toList())
                .build();
    }

    public DailyPreparationAssignmentResponse toAssignmentResponse(
            DailyPreparationAssignment entity
    ) {
        return DailyPreparationAssignmentResponse.builder()
                .id(entity.getId())
                .preparationId(entity.getPreparationId())
                .employeeId(entity.getEmployeeId())
                .employeeCode(entity.getEmployeeCode())
                .employeeName(entity.getEmployeeName())
                .customerId(entity.getCustomerId())
                .customerSiteId(entity.getCustomerSiteId())
                .customerName(entity.getCustomerName())
                .siteName(entity.getSiteName())
                .workDescription(entity.getWorkDescription())
                .build();
    }

    public DailyPreparationDispatchResponse toDispatchResponse(
            DailyPreparationDispatch entity
    ) {
        return DailyPreparationDispatchResponse.builder()
                .id(entity.getId())
                .preparationId(entity.getPreparationId())
                .customerId(entity.getCustomerId())
                .customerSiteId(entity.getCustomerSiteId())
                .customerName(entity.getCustomerName())
                .siteName(entity.getSiteName())
                .distanceFromCompanyKm(entity.getDistanceFromCompanyKm())
                .vehicleCount(entity.getVehicleCount())
                .note(entity.getNote())
                .build();
    }
}