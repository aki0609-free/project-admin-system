package com.project.backend.features.employee.dto;

import java.time.LocalDate;
import java.util.List;

public record EmployeeResignRequest(
        LocalDate resignDate,
        List<Long> checkedChecklistIds,
        String note
) {
}