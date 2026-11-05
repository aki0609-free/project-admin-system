package com.project.backend.features.system.masterdata.dto;

import com.project.backend.features.system.masterdata.entity.GenericMasterFieldType;

public record GenericMasterFieldResponse(
        Long id,
        String fieldKey,
        String fieldLabel,
        GenericMasterFieldType fieldType,
        boolean requiredFlag,
        boolean listVisibleFlag,
        boolean searchableFlag,
        int displayOrder,
        String unit,
        String placeholder,
        String validationPattern,
        String optionsJson
) {
}
