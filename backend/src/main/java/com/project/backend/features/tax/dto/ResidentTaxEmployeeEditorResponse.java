package com.project.backend.features.tax.dto;

import java.util.List;

public record ResidentTaxEmployeeEditorResponse(
        Long employeeId,
        String employeeCode,
        String employeeName,
        List<ResidentTaxMonthEditorResponse> months) {}
