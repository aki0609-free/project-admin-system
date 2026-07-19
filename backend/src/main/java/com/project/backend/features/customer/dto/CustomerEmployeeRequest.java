package com.project.backend.features.customer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerEmployeeRequest(
        Long id,
        String name,
        String furiganaName,
        String position,
        String phone,
        String email,
        Boolean invoiceToFlag,
        Boolean invoiceCcFlag,
        @JsonProperty("_isNew") Boolean isNew,
        @JsonProperty("_isUpdated") Boolean isUpdated,
        @JsonProperty("_isDeleted") Boolean isDeleted
) {
}