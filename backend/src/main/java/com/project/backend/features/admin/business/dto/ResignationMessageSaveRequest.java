package com.project.backend.features.admin.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResignationMessageSaveRequest(
        @NotBlank @Size(max = 200) String dialogTitle,
        @NotBlank @Size(max = 2000) String guidanceMessage,
        @NotBlank @Size(max = 500) String confirmationMessage
) {
}
