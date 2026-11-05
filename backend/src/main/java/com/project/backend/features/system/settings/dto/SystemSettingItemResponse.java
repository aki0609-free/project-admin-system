package com.project.backend.features.system.settings.dto;

public record SystemSettingItemResponse(
        Long definitionId,
        String largeCategoryKey,
        String largeCategoryName,
        String middleCategoryKey,
        String middleCategoryName,
        String smallCategoryKey,
        String settingName,
        String description,
        String valueType,
        boolean requiredFlag,
        String placeholder,
        int displayOrder,
        String value
) {
}
