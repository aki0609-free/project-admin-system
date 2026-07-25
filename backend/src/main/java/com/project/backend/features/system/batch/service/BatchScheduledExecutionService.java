package com.project.backend.features.system.batch.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchScheduledExecutionService {

    private final BatchExecutionService batchExecutionService;
    public void execute(String jobCode) {
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

        }
    }
}
