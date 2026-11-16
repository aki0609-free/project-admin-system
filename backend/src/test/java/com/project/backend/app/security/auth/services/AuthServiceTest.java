package com.project.backend.app.security.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import com.project.backend.app.security.auth.dto.AuthResponse;
import com.project.backend.app.security.auth.dto.AuthUserDto;
import com.project.backend.app.security.auth.dto.SecurityUser;
import com.project.backend.app.security.auth.mapper.AuthUserMapper;
import com.project.backend.app.security.auth.enums.LogoutReason;
import com.project.backend.app.audit.services.AuditLogService;
import com.project.backend.app.security.jwt.entity.RefreshToken;
import com.project.backend.app.security.jwt.services.CustomUserDetailsService;
import com.project.backend.app.security.jwt.services.JwtService;
import com.project.backend.app.security.jwt.services.RefreshTokenService;
import com.project.backend.features.user.entity.User;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private AuthUserMapper authUserMapper;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    @Test
    void refresh_shouldLoadUserFromStoredUserIdAndTenant() {
        String tokenValue = "refresh-token-uuid";
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(42L)
                .tenantId("default")
                .token(tokenValue)
                .build();
        SecurityUser securityUser = mock(SecurityUser.class);
        User user = mock(User.class);
        AuthUserDto userDto = new AuthUserDto(
                42L,
                "default",
                "operator",
                Set.of(),
                Set.of()
        );

        when(refreshTokenService.verifyToken(tokenValue))
                .thenReturn(refreshToken);
        when(customUserDetailsService.loadUserByIdAndTenantId(42L, "default"))
                .thenReturn(securityUser);
        when(securityUser.getUser()).thenReturn(user);
        when(authUserMapper.toAuthUserDto(user)).thenReturn(userDto);
        when(jwtService.generateToken(securityUser)).thenReturn("new-access-token");

        AuthResponse response = authService.refresh(tokenValue);

        assertThat(response.getUser()).isEqualTo(userDto);
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(tokenValue);
        verify(customUserDetailsService)
                .loadUserByIdAndTenantId(42L, "default");
    }

    @Test
    void logout_shouldRevokeOneSessionAndRecordSafeAuditIdentifier() {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(42L)
                .tenantId("default")
                .token("raw-refresh-token")
                .build();
        when(refreshTokenService.revokeToken("raw-refresh-token"))
                .thenReturn(java.util.Optional.of(refreshToken));

        authService.logout(
                "raw-refresh-token",
                LogoutReason.MANUAL,
                "Test Browser"
        );

        verify(refreshTokenService).revokeToken("raw-refresh-token");
        verify(auditLogService).recordLogout(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq("default"),
                org.mockito.ArgumentMatchers.eq("MANUAL"),
                org.mockito.ArgumentMatchers.matches("[0-9a-f]{16}"),
                org.mockito.ArgumentMatchers.eq("Test Browser"),
                org.mockito.ArgumentMatchers.eq(true)
        );
    }
}
