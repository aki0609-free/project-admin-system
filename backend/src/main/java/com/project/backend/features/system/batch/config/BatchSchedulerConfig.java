package com.project.backend.features.system.batch.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class BatchSchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler batchTaskScheduler(
            Clock applicationClock
    ) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setClock(applicationClock);
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("batch-job-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.initialize();
        return scheduler;
    }
}
