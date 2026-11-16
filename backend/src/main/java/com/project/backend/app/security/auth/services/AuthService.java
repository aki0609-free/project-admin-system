package com.project.backend.app.security.auth.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.project.backend.app.security.auth.dto.AuthResponse;
import com.project.backend.app.security.auth.dto.AuthUserDto;
import com.project.backend.app.security.auth.dto.LoginRequest;
import com.project.backend.app.security.auth.dto.SecurityUser;
import com.project.backend.app.security.auth.mapper.AuthUserMapper;
import com.project.backend.app.security.auth.enums.LogoutReason;
import com.project.backend.app.audit.services.AuditLogService;
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

    private final AuditLogService auditLogService;

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
            customUserDetailsService
                .loadUserByIdAndTenantId(
                    refreshToken.getUserId(),
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

    public void logout(
            String refreshTokenValue,
            LogoutReason reason,
            String deviceInfo
    ) {
        String sessionId = sessionId(refreshTokenValue);
        Optional<RefreshToken> revokedToken = refreshTokenService.revokeToken(
                refreshTokenValue
        );
        LogoutReason effectiveReason = revokedToken
                .filter(refreshTokenService::isExpired)
                .map(token -> LogoutReason.SESSION_EXPIRED)
                .orElse(reason);

        auditLogService.recordLogout(
                revokedToken.map(RefreshToken::getUserId).orElse(null),
                revokedToken.map(RefreshToken::getTenantId).orElse(null),
                effectiveReason.name(),
                sessionId,
                normalizeDeviceInfo(deviceInfo),
                revokedToken.isPresent()
        );
    }

    private String sessionId(String refreshTokenValue) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshTokenValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String normalizeDeviceInfo(String deviceInfo) {
        if (deviceInfo == null || deviceInfo.isBlank()) {
            return "UNKNOWN";
        }
        return deviceInfo.length() <= 500
                ? deviceInfo
                : deviceInfo.substring(0, 500);
    }
}
