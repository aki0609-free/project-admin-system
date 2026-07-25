package com.project.backend.features.system.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.dto.BatchJobRunResult;
import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchJobType;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;
import com.project.backend.features.system.batch.service.builder.BatchExecutionResponseBuilder;
import com.project.backend.features.system.batch.service.executor.BatchJobExecutor;
import com.project.backend.features.system.batch.service.executor.BatchJobExecutorResolver;

class BatchExecutionServiceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void executeNowForResult_shouldUseApplicationClockForCompletion() {
        Instant fixedInstant =
                Instant.parse("2026-07-31T15:30:00Z");
        BatchJobDefinition definition = definition();
        BatchExecutionLog executionLog =
                new BatchExecutionLog();
        executionLog.setId(10L);
        BatchJobExecutionResult executionResult =
                BatchJobExecutionResult.message("completed");

        BatchJobDefinitionLookupService lookupService =
                mock(BatchJobDefinitionLookupService.class);
        BatchJobDefinitionRepository definitionRepository =
                mock(BatchJobDefinitionRepository.class);
        BatchExecutionLogService logService =
                mock(BatchExecutionLogService.class);
        BatchJobExecutorResolver executorResolver =
                mock(BatchJobExecutorResolver.class);
        BatchJobExecutor executor =
                mock(BatchJobExecutor.class);
        BatchExecutionParameterService parameterService =
                mock(BatchExecutionParameterService.class);
        RedisBatchJobLockService lockService =
                mock(RedisBatchJobLockService.class);

        when(lookupService.findActiveByJobCode("TEST_JOB"))
                .thenReturn(definition);
        when(parameterService.validateAndNormalize(Map.of()))
                .thenReturn(Map.of());
        when(lockService.tryLock(
                any(String.class),
                any(java.time.Duration.class)
        )).thenReturn("lock-value");
        when(logService.start(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(executionLog);
        when(executorResolver.resolve(BatchJobType.BACKUP))
                .thenReturn(executor);
        when(executor.execute(any()))
                .thenReturn(executionResult);

        BatchExecutionService service =
                new BatchExecutionService(
                        lookupService,
                        definitionRepository,
                        logService,
                        executorResolver,
                        mock(BatchExecutionResponseBuilder.class),
                        parameterService,
                        lockService,
                        Clock.fixed(
                                fixedInstant,
                                ZoneId.of("Asia/Tokyo")
                        )
                );

        TenantContext.setTenantId("tenant-a");

        BatchJobRunResult result =
                service.executeNowForResult(
                        "TEST_JOB",
                        Map.of()
                );

        assertThat(result.executionLogId()).isEqualTo(10L);
        assertThat(definition.getLastExecutedAt())
                .isEqualTo(fixedInstant);
        verify(logService).complete(
                executionLog,
                executionResult,
                fixedInstant
        );
        verify(definitionRepository).save(definition);
        verify(lockService).unlock(
                "batch:execution:tenant-a:TEST_JOB",
                "lock-value"
        );
    }

    private BatchJobDefinition definition() {
        BatchJobDefinition definition =
                new BatchJobDefinition();
        definition.setTenantId("tenant-a");
        definition.setJobCode("TEST_JOB");
        definition.setJobName("テストジョブ");
        definition.setJobType(BatchJobType.BACKUP);
        definition.setTargetCode("TEST_TARGET");
        definition.setImmediateExecutable(true);
        return definition;
    }
}
