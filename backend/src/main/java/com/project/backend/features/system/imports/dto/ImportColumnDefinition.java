package com.project.backend.features.system.imports.dto;

import com.project.backend.features.system.imports.enums.ImportDataType;
import lombok.Builder;

@Builder
public record ImportColumnDefinition(
        Long id,
        String columnName,
        String csvHeaderName,
        ImportDataType dataType,
        boolean requiredFlag,
        boolean keyFlag,
        boolean nullableFlag,
        boolean trimFlag,
        String defaultValue,
        String formatPattern,
        boolean updatableFlag,
        int orderNo
) {
}