package com.project.backend.app.security.auth.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.project.backend.app.security.auth.dto.AuthResponse;
import com.project.backend.app.security.auth.dto.AuthUserDto;
import com.project.backend.app.security.auth.dto.LoginRequest;
import com.project.backend.app.security.auth.dto.SecurityUser;
import com.project.backend.app.security.auth.mapper.AuthUserMapper;
import com.project.backend.app.security.jwt.entity.RefreshToken;
import com.project.backend.app.security.jwt.services.CustomUserDetailsService;
import com.project.backend.app.security.jwt.services.JwtService;
import com.project.backend.app.security.jwt.services.RefreshTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final RefreshTokenService refreshTokenService;

    private final CustomUserDetailsService customUserDetailsService;

    private final AuthUserMapper authUserMapper;

    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        
        AuthUserDto authUserDto = authUserMapper.toAuthUserDto(user.getUser());
        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createToken(user).getToken();

        return new AuthResponse(authUserDto, accessToken, refreshToken);
    }

    public AuthResponse refresh(String refreshTokenValue) {

        RefreshToken refreshToken =
            refreshTokenService.verifyToken(refreshTokenValue);
        
        SecurityUser user =
            (SecurityUser) customUserDetailsService
                .loadUserByUsernameAndTenantId(
                    jwtService.extractUsername(refreshTokenValue),
                    refreshToken.getTenantId()
                );
        
        AuthUserDto authUserDto = authUserMapper.toAuthUserDto(user.getUser());
        String accessToken = jwtService.generateToken(user);

        return new AuthResponse(authUserDto, accessToken, refreshTokenValue);
    }

    public AuthUserDto me(Authentication authentication) {
        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        return authUserMapper.toAuthUserDto(user.getUser());
    }
}
