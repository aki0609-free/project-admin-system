package com.project.backend.features.system.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class BatchSchedulerConfigTest {

    @Test
    void batchScheduler_shouldUseApplicationClock() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-07-31T15:30:00Z"),
                ZoneId.of("Asia/Tokyo")
        );

        ThreadPoolTaskScheduler scheduler =
                new BatchSchedulerConfig()
                        .batchTaskScheduler(fixedClock);

        try {
            assertThat(scheduler.getClock())
                    .isSameAs(fixedClock);
            assertThat(scheduler.getClock().getZone())
                    .isEqualTo(ZoneId.of("Asia/Tokyo"));
        } finally {
            scheduler.shutdown();
        }
    }
}
