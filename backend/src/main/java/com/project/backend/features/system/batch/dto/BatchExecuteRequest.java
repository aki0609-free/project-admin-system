package com.project.backend.features.system.batch.dto;

import java.util.Map;

public record BatchExecuteRequest(
        Map<String, Object> params
) {
}