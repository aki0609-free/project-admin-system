package com.project.backend.features.admin.business.dto;

public record ResignationChecklistAdminResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean requiredFlag,
        int displayOrder,
        boolean activeFlag
) {
}
