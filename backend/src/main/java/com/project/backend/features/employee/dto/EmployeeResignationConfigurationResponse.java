package com.project.backend.features.employee.dto;

import java.util.List;

public record EmployeeResignationConfigurationResponse(
        EmployeeResignationMessageResponse message,
        List<EmployeeResignationChecklistResponse> checklist
) {
}
