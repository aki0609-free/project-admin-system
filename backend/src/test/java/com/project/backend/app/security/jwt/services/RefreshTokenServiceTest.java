package com.project.backend.app.security.jwt.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.app.security.jwt.entity.RefreshToken;
import com.project.backend.app.security.jwt.exception.InvalidRefreshTokenException;
import com.project.backend.app.security.jwt.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void verifyToken_shouldRejectUnknownTokenAsAuthenticationError() {
        when(repository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyToken("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void verifyToken_shouldDeleteAndRejectExpiredToken() {
        RefreshToken expired = RefreshToken.builder()
                .token("expired")
                .expiryDate(Instant.now().minusSeconds(1))
                .build();
        when(repository.findByToken("expired")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.verifyToken("expired"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository).delete(expired);
    }
}
