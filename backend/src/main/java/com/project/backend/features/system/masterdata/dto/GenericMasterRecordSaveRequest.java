package com.project.backend.features.system.masterdata.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record GenericMasterRecordSaveRequest(
        @NotNull Map<String, Object> values,
        boolean activeFlag,
        Long lockVersion
) {
}
