package com.project.backend.app.security.jwt.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.project.backend.app.security.auth.dto.SecurityUser;
import com.project.backend.app.security.jwt.entity.RefreshToken;
import com.project.backend.app.security.jwt.exception.InvalidRefreshTokenException;
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
        RefreshToken token = RefreshToken.builder()
                .userId(user.getUserId())
                .tenantId(user.getTenantId())
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        
        return repository.save(token);
    }

    @Transactional
    public Optional<RefreshToken> revokeToken(String token) {
        Optional<RefreshToken> refreshToken = repository.findByToken(token);
        refreshToken.ifPresent(repository::delete);
        return refreshToken;
    }

    @SuppressWarnings("null")
    public RefreshToken verifyToken(String token) {

        RefreshToken refreshToken = repository
            .findByToken(token)
            .orElseThrow(InvalidRefreshTokenException::new);
        
        if (isExpired(refreshToken)) {
            // クライアントが続けて /auth/logout を呼び出した際に、
            // 失効対象のユーザーと端末セッションを監査できるよう、ここでは
            // 削除しない。期限切れトークンは検証には必ず失敗し、logout時に
            // 該当レコードだけを削除する。
            throw new InvalidRefreshTokenException();
        }

        return refreshToken;
    }

    public boolean isExpired(RefreshToken token) {
        return token.getExpiryDate().isBefore(Instant.now());
    }
    
}
