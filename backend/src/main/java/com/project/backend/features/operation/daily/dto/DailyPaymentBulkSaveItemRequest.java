package com.project.backend.features.operation.daily.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.backend.features.operation.daily.enums.DailyPaymentStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyPaymentBulkSaveItemRequest {

    private Long id;

    private Long employeeId;

    private BigDecimal plannedAmount;

    private BigDecimal actualAmount;

    private DailyPaymentStatus status;

    private String note;

    @JsonProperty("isNew")
    private boolean newFlag;

    @JsonProperty("isUpdated")
    private boolean updatedFlag;

    @JsonProperty("isDeleted")
    private boolean deletedFlag;

    public boolean isNew() {
        return newFlag;
    }

    public boolean isUpdated() {
        return updatedFlag;
    }

    public boolean isDeleted() {
        return deletedFlag;
    }
}