package com.project.backend.features.application.dto;

import java.math.BigDecimal;

public record ApplicationMediaBulkUpdateItem(
        Long id,
        String mediaName,
        String mediaArea,
        Integer mediaSlots,
        String mediaYearMonth,
        BigDecimal cost
) {
}