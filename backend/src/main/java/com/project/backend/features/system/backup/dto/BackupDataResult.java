package com.project.backend.features.system.backup.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;

@Builder
public record BackupDataResult(
        List<String> columns,
        List<Map<String, Object>> rows
) {
}