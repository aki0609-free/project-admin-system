package com.project.backend.features.tax.dto;

import java.util.List;

public record StandardRemunerationEditorResponse(
        Long batchId,
        String status,
        boolean hasClosedPeriodChanges,
        List<StandardRemunerationEmployeeOption> employees,
        List<StandardRemunerationEditorRowResponse> rows) {}
