package com.project.backend.features.application.dto;

import java.util.List;

public record ApplicationMediaBulkSaveRequest(
    List<ApplicationMediaCreateRequest> created,
    List<ApplicationMediaBulkUpdateItem> updated,
    List<Long> deletedIds
) {}
