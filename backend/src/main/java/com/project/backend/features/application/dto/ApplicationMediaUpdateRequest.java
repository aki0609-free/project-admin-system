package com.project.backend.features.application.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationMediaUpdateRequest {
    private String mediaName;
    private String mediaArea;
    private Integer mediaSlots;
    private String mediaYearMonth;
    private BigDecimal cost;
}
