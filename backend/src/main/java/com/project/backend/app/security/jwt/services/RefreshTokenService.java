package com.project.backend.app.security.jwt.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.project.backend.app.security.auth.dto.SecurityUser;
import com.project.backend.app.security.jwt.entity.RefreshToken;
import com.project.backend.app.security.jwt.repository.RefreshTokenRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    @SuppressWarnings("null")
    @Transactional
    public RefreshToken createToken(SecurityUser user) {

        repository.deleteByUserId(user.getUserId());

        RefreshToken token = RefreshToken.builder()
                .userId(user.getUserId())
                .tenantId(user.getTenantId())
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        
        return repository.save(token);
    }

    @SuppressWarnings("null")
    public RefreshToken verifyToken(String token) {

        RefreshToken refreshToken = repository
            .findByToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        
        if (isExpired(refreshToken)) {
            repository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }
    
}
