package com.project.backend.app.security.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private AuthUserDto user;
    
    private String accessToken;

    private String refreshToken;
    
}
