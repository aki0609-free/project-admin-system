package com.project.backend.features.employee.dto;

public record EmployeeResignationMessageResponse(
        String dialogTitle,
        String guidanceMessage,
        String confirmationMessage
) {
}
