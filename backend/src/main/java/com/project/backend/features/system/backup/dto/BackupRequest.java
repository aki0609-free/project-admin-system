package com.project.backend.features.system.backup.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record BackupRequest(
        List<String> targetCodes,
        boolean includeHeader,
        String encoding,
        Boolean zipOutput
) {
}