package com.project.backend.features.system.batch.service;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisBatchJobLockService {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] "
                            + "then return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class
            );

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
        redisTemplate.execute(
                UNLOCK_SCRIPT,
                List.of(lockKey),
                lockValue
        );
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
