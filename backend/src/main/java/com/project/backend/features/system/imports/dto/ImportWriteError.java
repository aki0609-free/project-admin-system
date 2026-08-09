package com.project.backend.features.system.imports.dto;

import java.io.Serializable;

import lombok.Builder;

@Builder
public record ImportWriteError(
        int rowNo,
        String csvHeaderName,
        String columnName,
        String rawValue,
        String errorMessage
) implements Serializable {
}
