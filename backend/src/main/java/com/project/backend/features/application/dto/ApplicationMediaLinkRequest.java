package com.project.backend.features.application.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationMediaLinkRequest {
    private Long id;
    private String mediaName;
    private String mediaArea;
    private Integer mediaSlots;
    private String mediaYearMonth;
}