package com.project.backend.features.tax.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StandardRemunerationRowInput(
        Long currentRecordId,
        Long employeeId,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        BigDecimal healthStandardRemuneration,
        BigDecimal pensionStandardRemuneration,
        String sourceType,
        String note) {}
