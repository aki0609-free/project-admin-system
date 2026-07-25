package com.project.backend.features.system.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchScheduleType;

class BatchDynamicSchedulerServiceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @SuppressWarnings({"unchecked", "null"})
    @Test
    void scheduledTaskCarriesTenantContextAndClearsItAfterExecution() {
        BatchScheduleTargetQueryService targetQueryService =
                mock(BatchScheduleTargetQueryService.class);
        BatchScheduledExecutionService executionService =
                mock(BatchScheduledExecutionService.class);
        ThreadPoolTaskScheduler scheduler =
                mock(ThreadPoolTaskScheduler.class);
        ScheduledFuture<Object> future = mock(ScheduledFuture.class);

        BatchJobDefinition definition = new BatchJobDefinition();
        definition.setId(10L);
        definition.setTenantId("tenant-a");
        definition.setJobCode("REPORT_MONTHLY");
        definition.setScheduleType(BatchScheduleType.CRON);
        definition.setCronExpression("0 0 9 * * *");

        when(targetQueryService.findScheduleTargets())
                .thenReturn(List.of(definition));
        doReturn(future)
                .when(scheduler)
                .schedule(any(Runnable.class), any(Trigger.class));

        BatchDynamicSchedulerService service =
                new BatchDynamicSchedulerService(
                        targetQueryService,
                        executionService,
                        scheduler
                );

        service.reloadSchedules();

        ArgumentCaptor<Runnable> runnableCaptor =
                ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(
                runnableCaptor.capture(),
                any(Trigger.class)
        );

        runnableCaptor.getValue().run();

        verify(executionService).execute("REPORT_MONTHLY");
        assertThat(TenantContext.getTenantId()).isNull();
    }
}
