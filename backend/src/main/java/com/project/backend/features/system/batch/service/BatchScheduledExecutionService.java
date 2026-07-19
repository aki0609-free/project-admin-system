package com.project.backend.features.system.batch.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchScheduledExecutionService {

    private final BatchExecutionService batchExecutionService;
    private final RedisBatchJobLockService lockService;

    public void execute(String jobCode) {
        String lockKey = "batch:schedule:" + jobCode;

        String lockValue = lockService.tryLock(
                lockKey,
                Duration.ofMinutes(30)
        );

        if (lockValue == null) {
            log.info(
                    "Batch skipped because lock already exists. jobCode={}",
                    jobCode
            );
            return;
        }

        try {
            log.info(
                    "Scheduled batch started. jobCode={}",
                    jobCode
            );

            batchExecutionService.executeScheduled(jobCode);

            log.info(
                    "Scheduled batch finished. jobCode={}",
                    jobCode
            );

        } catch (Exception e) {
            log.error(
                    "Scheduled batch failed. jobCode={}",
                    jobCode,
                    e
            );

        } finally {
            lockService.unlock(lockKey, lockValue);
        }
    }
}