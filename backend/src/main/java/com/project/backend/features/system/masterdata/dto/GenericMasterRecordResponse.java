package com.project.backend.features.system.masterdata.dto;

import java.time.Instant;
import java.util.Map;

public record GenericMasterRecordResponse(
        Long id,
        Long definitionId,
        Map<String, Object> values,
        boolean activeFlag,
        long lockVersion,
        Instant updatedAt
) {
}
