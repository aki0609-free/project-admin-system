package com.project.backend.features.employee.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeResignRequest(
        @NotNull LocalDate resignDate,
        List<Long> checkedChecklistIds,
        @Size(max = 1000) String note
) {
}
