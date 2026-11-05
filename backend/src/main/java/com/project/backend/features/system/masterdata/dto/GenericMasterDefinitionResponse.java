package com.project.backend.features.system.masterdata.dto;

import java.util.List;

import com.project.backend.features.system.masterdata.entity.GenericMasterPlacement;
import com.project.backend.features.system.masterdata.entity.GenericMasterStatus;

public record GenericMasterDefinitionResponse(
        Long id,
        String masterCode,
        String masterName,
        String tabName,
        GenericMasterPlacement placement,
        String description,
        int displayOrder,
        GenericMasterStatus status,
        boolean activeFlag,
        List<GenericMasterFieldResponse> fields
) {
}
