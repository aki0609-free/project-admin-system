package com.project.backend.app.security.auth.dto;

import com.project.backend.app.security.auth.enums.LogoutReason;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogoutRequest {

    @NotBlank
    private String refreshToken;

    @NotNull
    private LogoutReason reason;
}
