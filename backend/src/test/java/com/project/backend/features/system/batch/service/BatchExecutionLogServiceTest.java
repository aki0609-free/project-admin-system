package com.project.backend.features.system.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchExecutionStatus;
import com.project.backend.features.system.batch.enums.BatchExecutionTrigger;
import com.project.backend.features.system.batch.mapper.BatchJobMapper;
import com.project.backend.features.system.batch.repository.BatchExecutionLogRepository;

class BatchExecutionLogServiceTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-07-31T15:30:00Z");

    private BatchExecutionLogRepository repository;
    private BatchExecutionLogService service;

    @BeforeEach
    void setUp() {
        repository = mock(BatchExecutionLogRepository.class);
        when(repository.save(any(BatchExecutionLog.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        BatchExecutionParameterService parameterService =
                mock(BatchExecutionParameterService.class);
        when(parameterService.serialize(anyMap()))
                .thenReturn("{}");

        service = new BatchExecutionLogService(
                repository,
                mock(BatchJobMapper.class),
                parameterService,
                Clock.fixed(
                        FIXED_INSTANT,
                        ZoneId.of("Asia/Tokyo")
                )
        );
    }

    @Test
    void start_shouldUseApplicationClock() {
        BatchExecutionLog log = service.start(
                definition(),
                BatchExecutionTrigger.SCHEDULED,
                Map.of(),
                null
        );

        assertThat(log.getStatus())
                .isEqualTo(BatchExecutionStatus.STARTED);
        assertThat(log.getStartedAt())
                .isEqualTo(FIXED_INSTANT);
        assertThat(log.getExecutedBy())
                .isEqualTo("system");
    }

    @Test
    void fail_shouldUseApplicationClock() {
        BatchExecutionLog log = new BatchExecutionLog();

        service.fail(
                log,
                new IllegalStateException("failed")
        );

        assertThat(log.getStatus())
                .isEqualTo(BatchExecutionStatus.FAILED);
        assertThat(log.getFinishedAt())
                .isEqualTo(FIXED_INSTANT);
        assertThat(log.getErrorMessage())
                .isEqualTo("failed");
    }

    private BatchJobDefinition definition() {
        BatchJobDefinition definition =
                new BatchJobDefinition();
        definition.setJobCode("TEST_JOB");
        definition.setJobName("テストジョブ");
        return definition;
    }
}
