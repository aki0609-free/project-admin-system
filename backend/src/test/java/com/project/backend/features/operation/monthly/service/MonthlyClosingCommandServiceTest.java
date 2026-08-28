package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingExecution;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.mapper.MonthlyClosingMapper;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;

class MonthlyClosingCommandServiceTest {

    private MonthlyClosingRepository repository;
    private MonthlyClosingWorkflowService workflowService;
    private MonthlyClosingPeriodService periodService;
    private MonthlyClosingOutputDefinitionService definitionService;
    private MonthlyClosingExecutionStateService stateService;
    private MonthlyClosingCommandService service;
    private MonthlyClosing entity;
    private MonthlyClosingPeriod period;
    private List<MonthlyClosingOutputDefinition> definitions;

    @BeforeEach
    void setUp() {
        repository = mock(MonthlyClosingRepository.class);
        workflowService = mock(MonthlyClosingWorkflowService.class);
        periodService = mock(MonthlyClosingPeriodService.class);
        definitionService = mock(
                MonthlyClosingOutputDefinitionService.class
        );
        stateService = mock(MonthlyClosingExecutionStateService.class);

        entity = new MonthlyClosing();
        entity.setId(10L);
        entity.setTargetMonth(LocalDate.of(2026, 7, 1));
        entity.setStatus(MonthlyClosingStatus.OPEN);
        entity.setClosingVersion(0);
        period = new MonthlyClosingPeriod(
                "2026-07",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                null
        );
        MonthlyClosingOutputDefinition definition =
                new MonthlyClosingOutputDefinition();
        definition.setOutputType(MonthlyClosingOutputType.REPORT);
        definition.setOutputCode("MONTHLY_PAY_SLIP");
        definitions = List.of(definition);

        when(repository.findByTargetMonthAndDeletedAtIsNull(
                LocalDate.of(2026, 7, 1)
        )).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        when(periodService.resolve("2026-07")).thenReturn(period);
        when(definitionService.findActiveCompanyOutputs())
                .thenReturn(definitions);
        when(stateService.nextVersion(10L, 0)).thenReturn(1);
        MonthlyClosingExecution execution = new MonthlyClosingExecution();
        execution.setId(100L);
        when(stateService.startNew(
                10L,
                1,
                "SYSTEM",
                definitions
        )).thenReturn(execution);

        service = new MonthlyClosingCommandService(
                repository,
                mock(MonthlyClosingMapper.class),
                workflowService,
                periodService,
                definitionService,
                stateService
        );
    }

    @Test
    void close_shouldCompleteExecutionOnlyAfterWorkflowCompletes() {
        service.close("2026-07");

        verify(workflowService).execute(10L, period, 1, definitions);
        verify(stateService).completeItems(100L);
        verify(stateService).complete(100L);
        verify(stateService, never()).fail(any(), any());
    }

    @Test
    void close_shouldPersistFailedExecutionWhenWorkflowFails() {
        IllegalStateException failure =
                new IllegalStateException("帳票生成失敗");
        doThrow(failure).when(workflowService)
                .execute(10L, period, 1, definitions);

        assertThatThrownBy(() -> service.close("2026-07"))
                .isSameAs(failure);

        verify(stateService).fail(100L, failure);
        verify(stateService, never()).completeItems(any());
        verify(stateService, never()).complete(any());
    }

    @Test
    void close_shouldRejectClosingAlreadyCompleted() {
        entity.setStatus(MonthlyClosingStatus.CLOSED);
        entity.setClosingVersion(1);

        assertThatThrownBy(() -> service.close("2026-07"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("再締め");
        verify(stateService, never()).startNew(
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void reclose_shouldUseVersionAfterFailedAttempt() {
        entity.setStatus(MonthlyClosingStatus.FAILED);
        entity.setClosingVersion(2);
        when(stateService.nextVersion(10L, 2)).thenReturn(4);
        MonthlyClosingExecution execution = new MonthlyClosingExecution();
        execution.setId(400L);
        when(stateService.startNew(
                10L,
                4,
                "SYSTEM",
                definitions
        )).thenReturn(execution);

        service.reclose("2026-07");

        verify(workflowService).execute(10L, period, 4, definitions);
        verify(stateService).completeItems(400L);
        verify(stateService).complete(400L);
    }
}
