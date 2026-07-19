package com.project.backend.features.system.batch.service;

import java.net.InetAddress;
import java.time.Duration;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisBatchJobLockService {

    private final StringRedisTemplate redisTemplate;

    @SuppressWarnings("null")
    public String tryLock(String lockKey, Duration ttl) {
        String lockValue = buildLockValue();

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, ttl);

        if (Boolean.TRUE.equals(success)) {
            return lockValue;
        }

        return null;
    }

    @SuppressWarnings("null")
    public void unlock(String lockKey, String lockValue) {
        String currentValue = redisTemplate.opsForValue().get(lockKey);

        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
        }
    }

    private String buildLockValue() {
        return resolveHostName() + ":" + UUID.randomUUID();
    }

    private String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}