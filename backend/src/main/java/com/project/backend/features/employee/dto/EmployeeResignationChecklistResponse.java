package com.project.backend.features.employee.dto;

import lombok.Builder;

@Builder
public record EmployeeResignationChecklistResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean requiredFlag,
        int displayOrder
) {
}