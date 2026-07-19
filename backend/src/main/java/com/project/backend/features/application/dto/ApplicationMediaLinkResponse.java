package com.project.backend.features.application.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationMediaLinkResponse {
    private Long id;
    private String mediaName;
    private String mediaArea;
    private Integer mediaSlots;
    private String mediaYearMonth;
    private BigDecimal unitPrice;
}
