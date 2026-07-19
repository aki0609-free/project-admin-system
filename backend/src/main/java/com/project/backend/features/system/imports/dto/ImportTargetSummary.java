package com.project.backend.features.system.imports.dto;

import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.enums.ImportSourceType;
import lombok.Builder;

@Builder
public record ImportTargetSummary(
        Long id,
        String targetCode,
        String targetName,
        String tableName,
        String description,
        ImportSourceType sourceType,
        String fixedFilePath,
        ImportScriptType scriptType,
        String scriptPath,
        String scriptArgs,
        ImportMode importMode,
        Integer headerRowNumber,
        Integer dataStartRowNumber,
        String charset,
        String delimiter,
        boolean activeFlag
) {
}