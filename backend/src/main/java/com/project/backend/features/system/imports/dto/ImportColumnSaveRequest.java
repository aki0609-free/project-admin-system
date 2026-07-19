package com.project.backend.features.system.imports.dto;

import com.project.backend.features.system.imports.enums.ImportDataType;

import lombok.Builder;

@Builder
public record ImportColumnSaveRequest(
        Long id,
        String columnName,
        String csvHeaderName,
        ImportDataType dataType,
        Boolean requiredFlag,
        Boolean keyFlag,
        Boolean nullableFlag,
        Boolean trimFlag,
        String defaultValue,
        String formatPattern,
        Boolean updatableFlag,
        Integer orderNo
) {
    public boolean requiredFlagOrDefault() {
        return Boolean.TRUE.equals(requiredFlag);
    }

    public boolean keyFlagOrDefault() {
        return Boolean.TRUE.equals(keyFlag);
    }

    public boolean nullableFlagOrDefault() {
        return nullableFlag == null || nullableFlag;
    }

    public boolean trimFlagOrDefault() {
        return trimFlag == null || trimFlag;
    }

    public boolean updatableFlagOrDefault() {
        return updatableFlag == null || updatableFlag;
    }

    public int orderNoOrDefault() {
        return orderNo != null ? orderNo : 1;
    }
}