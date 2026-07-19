package com.project.backend.app.security.permission.services;

import java.time.Duration;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("null")
    public void cachePermissions(Long userId, Set<String> permissions) {

        redisTemplate.opsForValue().set(
            "perm:" + userId,
            permissions,
            Duration.ofMinutes(30)
        );
    }

    @SuppressWarnings("unchecked")
    public Set<String> getPermissions(Long userId) {

        return (Set<String>) redisTemplate
                .opsForValue()
                .get("perm:" + userId);
    }
    
}
