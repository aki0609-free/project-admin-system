package com.project.backend.features.operation.preparation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyPreparationDispatchBulkSaveItemRequest {

    private Long id;

    private Long customerId;

    private Long customerSiteId;

    private Integer vehicleCount;

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