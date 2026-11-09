package com.project.backend.features.customer.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerSiteRequest(
        Long id,
        String name,
        String contactPersonName,
        String contactPersonPhone,
        String contactPersonEmail,
        BigDecimal distanceFromCompanyKm,
        @JsonProperty("_isNew") Boolean isNew,
        @JsonProperty("_isUpdated") Boolean isUpdated,
        @JsonProperty("_isDeleted") Boolean isDeleted
) {
}
