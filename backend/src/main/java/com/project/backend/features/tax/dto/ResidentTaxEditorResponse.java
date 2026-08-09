package com.project.backend.features.tax.dto;

import java.util.List;

public record ResidentTaxEditorResponse(
        Long batchId,
        Integer fiscalYear,
        String status,
        boolean hasClosedMonthChanges,
        List<ResidentTaxEmployeeEditorResponse> employees) {}
