package com.project.backend.features.system.batch.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchScheduleType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchDynamicSchedulerService {

    private final BatchScheduleTargetQueryService targetQueryService;
    private final BatchScheduledExecutionService scheduledExecutionService;
    private final ThreadPoolTaskScheduler batchTaskScheduler;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks =
            new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        reloadSchedules();
    }

    @PreDestroy
    public void shutdown() {
        cancelAll();
    }

    public synchronized void reloadSchedules() {
        cancelAll();

        targetQueryService.findScheduleTargets()
                .forEach(this::register);

        log.info(
                "Batch schedules reloaded. count={}",
                scheduledTasks.size()
        );
    }

    public synchronized void reloadJob(Long id) {
        cancel(id);

        BatchJobDefinition definition =
                targetQueryService.findScheduleTargetOrNull(id);

        if (definition == null) {
            return;
        }

        register(definition);
    }

    public synchronized void cancel(Long id) {
        ScheduledFuture<?> future =
                scheduledTasks.remove(id);

        if (future != null) {
            future.cancel(false);
        }
    }

    public synchronized void cancelAll() {
        scheduledTasks.values()
                .forEach(future -> future.cancel(false));

        scheduledTasks.clear();
    }

    @SuppressWarnings("null")
    private void register(BatchJobDefinition definition) {
        if (definition.getId() == null) {
            return;
        }

        if (definition.getScheduleType() != BatchScheduleType.CRON) {
            return;
        }

        if (!StringUtils.hasText(definition.getCronExpression())) {
            log.warn(
                    "Batch schedule skipped because cronExpression is empty. jobCode={}",
                    definition.getJobCode()
            );
            return;
        }

        try {
            CronTrigger trigger =
                    new CronTrigger(definition.getCronExpression());

            ScheduledFuture<?> future =
                    batchTaskScheduler.schedule(
                            () -> scheduledExecutionService.execute(
                                    definition.getJobCode()
                            ),
                            trigger
                    );

            if (future != null) {
                scheduledTasks.put(
                        definition.getId(),
                        future
                );
            }

            log.info(
                    "Batch scheduled. id={}, jobCode={}, cron={}",
                    definition.getId(),
                    definition.getJobCode(),
                    definition.getCronExpression()
            );

        } catch (Exception e) {
            log.error(
                    "Batch schedule registration failed. id={}, jobCode={}, cron={}",
                    definition.getId(),
                    definition.getJobCode(),
                    definition.getCronExpression(),
                    e
            );
        }
    }
}