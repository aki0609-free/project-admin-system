package com.project.backend.features.admin.business.dto;

import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.operation.reportpreview.enums.OperationType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PreviewReportSettingSaveRequest(
        Long id,
        @NotNull OperationType operationType,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,99}$")
        String reportCode,
        @NotBlank @Size(max = 200) String reportName,
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_]+$")
        String tableName,
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,99}$")
        String filterColumnName,
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,99}$")
        String targetParamName,
        @Pattern(regexp = "^[A-Za-z0-9_,\\s]*$")
        @Size(max = 500)
        String orderBy,
        @NotNull @Min(1) Integer displayOrder,
        @NotNull OperationReportOutputType outputType,
        @NotNull Boolean activeFlag
) {
}
