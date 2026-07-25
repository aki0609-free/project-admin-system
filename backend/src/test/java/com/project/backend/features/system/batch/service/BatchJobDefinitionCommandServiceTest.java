package com.project.backend.features.system.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.mapper.BatchJobMapper;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;
import com.project.backend.features.system.batch.service.validation.BatchJobDefinitionValidator;

class BatchJobDefinitionCommandServiceTest {

    @Test
    void delete_shouldUseApplicationClockAndCancelSchedule() {
        Instant fixedInstant =
                Instant.parse("2026-07-31T15:30:00Z");
        BatchJobDefinitionLookupService lookupService =
                mock(BatchJobDefinitionLookupService.class);
        BatchDynamicSchedulerService schedulerService =
                mock(BatchDynamicSchedulerService.class);
        BatchJobDefinition definition =
                new BatchJobDefinition();
        definition.setId(1L);

        when(lookupService.find(1L))
                .thenReturn(definition);

        BatchJobDefinitionCommandService service =
                new BatchJobDefinitionCommandService(
                        mock(BatchJobDefinitionRepository.class),
                        mock(BatchJobMapper.class),
                        mock(BatchJobDefinitionValidator.class),
                        lookupService,
                        schedulerService,
                        Clock.fixed(
                                fixedInstant,
                                ZoneId.of("Asia/Tokyo")
                        )
                );

        service.delete(1L);

        assertThat(definition.getDeletedAt())
                .isEqualTo(fixedInstant);
        verify(schedulerService).cancel(1L);
    }
}
