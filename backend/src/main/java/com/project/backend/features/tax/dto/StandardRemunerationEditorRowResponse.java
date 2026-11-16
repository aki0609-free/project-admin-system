package com.project.backend.features.tax.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StandardRemunerationEditorRowResponse(
        Long currentRecordId,
        Long employeeId,
        String employeeCode,
        String employeeName,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal healthStandardRemuneration,
        BigDecimal pensionStandardRemuneration,
        String sourceType,
        String note,
        boolean changed,
        boolean closed) {}
