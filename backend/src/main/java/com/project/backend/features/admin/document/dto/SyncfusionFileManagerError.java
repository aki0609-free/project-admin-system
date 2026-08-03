package com.project.backend.features.admin.document.dto;

import java.util.List;

public record SyncfusionFileManagerError(
        String code,
        String message,
        List<String> fileExists
) {
}
