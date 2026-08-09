package com.project.backend.features.tax.dto;

public record ResidentTaxMonthEditorResponse(
        Integer month,
        Integer currentTaxAmount,
        Integer draftTaxAmount,
        boolean changed,
        boolean closed) {}
