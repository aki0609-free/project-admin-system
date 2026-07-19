package com.project.backend.features.system.backup.dto;

import java.util.List;

public record BackupExecuteRequest(
        List<String> targetCodes
) {
}