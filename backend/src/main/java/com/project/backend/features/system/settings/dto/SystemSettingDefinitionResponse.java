package com.project.backend.features.system.settings.dto;

public record SystemSettingDefinitionResponse(
        Long id,
        String scope,
        String largeCategoryKey,
        String largeCategoryName,
        String middleCategoryKey,
        String middleCategoryName,
        String smallCategoryKey,
        String settingName,
        String description,
        String valueType,
        boolean requiredFlag,
        String validationPattern,
        String placeholder,
        int displayOrder,
        boolean sensitiveFlag,
        boolean activeFlag,
        boolean systemManagedFlag,
        boolean editable,
        String settingValue
) {
}
