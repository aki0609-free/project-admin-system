package com.project.backend.app.security.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.app.security.auth.dto.AuthResponse;
import com.project.backend.app.security.auth.dto.AuthUserDto;
import com.project.backend.app.security.auth.dto.LoginRequest;
import com.project.backend.app.security.auth.services.AuthService;
import com.project.backend.app.security.jwt.dto.RefreshRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(
        @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
       @Valid @RequestBody RefreshRequest request
    ) {
        return authService.refresh(request.getRefreshToken());
    }

    @GetMapping("/me")
    public AuthUserDto me(Authentication authentication) {
        return authService.me(authentication);
    }
    
    
    
}
