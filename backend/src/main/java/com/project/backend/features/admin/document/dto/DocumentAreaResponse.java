package com.project.backend.features.admin.document.dto;

import java.util.Set;

import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.enums.DocumentOperation;

public record DocumentAreaResponse(
        DocumentArea area,
        String displayName,
        Set<DocumentOperation> allowedOperations
) {
}
