package com.project.backend.features.operation.preparation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyPreparationAssignmentBulkSaveItemRequest {

    private Long id;

    private Long employeeId;

    private Long customerId;

    private Long customerSiteId;

    private String workDescription;

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