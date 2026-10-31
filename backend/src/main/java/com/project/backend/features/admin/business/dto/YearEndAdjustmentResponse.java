package com.project.backend.features.admin.business.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record YearEndAdjustmentResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        Integer taxYear,
        YearMonth targetMonth,
        BigDecimal adjustmentAmount,
        String status,
        String note,
        Long lockVersion
) {}
