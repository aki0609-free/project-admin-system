package com.project.backend.features.operation.monthly.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerBillingTargetResponse(
        Long customerId,
        String customerName,
        String closingRuleLabel,
        LocalDate periodFrom,
        LocalDate periodTo,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        boolean calculationReady,
        boolean closingDateReached,
        CustomerBillingClosingResponse closing
) {
}
