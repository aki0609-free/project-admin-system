package com.project.backend.features.application.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationMediaCreateRequest {
    private String mediaName;
    private String mediaArea;
    private Integer mediaSlots;
    private String mediaYearMonth;
    private BigDecimal cost;
}
