package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingExecution;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingItem;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingExecutionStatus;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingItemStatus;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingExecutionRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingItemRepository;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;

class MonthlyClosingExecutionStateServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-07-30T01:02:03Z");

    private MonthlyClosingRepository closingRepository;
    private MonthlyClosingExecutionRepository executionRepository;
    private MonthlyClosingItemRepository itemRepository;
    private MonthlyClosingExecutionStateService service;

    @BeforeEach
    void setUp() {
        closingRepository = mock(MonthlyClosingRepository.class);
        executionRepository =
                mock(MonthlyClosingExecutionRepository.class);
        itemRepository = mock(MonthlyClosingItemRepository.class);
        service = new MonthlyClosingExecutionStateService(
                closingRepository,
                executionRepository,
                itemRepository,
                Clock.fixed(NOW, ZoneId.of("Asia/Tokyo"))
        );
    }

    @Test
    void startNew_shouldCreateExecutionAndItems() {
        MonthlyClosing closing = new MonthlyClosing();
        closing.setId(1L);
        when(closingRepository.findById(1L))
                .thenReturn(Optional.of(closing));
        when(executionRepository
                .findByMonthlyClosingIdAndClosingVersionAndDeletedAtIsNull(
                        1L,
                        2
                ))
                .thenReturn(Optional.empty());
        when(executionRepository.save(any()))
                .thenAnswer(invocation -> {
                    MonthlyClosingExecution value =
                            invocation.getArgument(0);
                    value.setId(10L);
                    return value;
                });

        MonthlyClosingOutputDefinition definition =
                new MonthlyClosingOutputDefinition();
        definition.setOutputType(MonthlyClosingOutputType.REPORT);
        definition.setOutputCode("MONTHLY_PAY_SLIP");
        definition.setRequiredFlag(true);

        MonthlyClosingExecution result = service.startNew(
                1L,
                2,
                "sys-admin",
                List.of(definition)
        );

        assertThat(result.getStartedAt()).isEqualTo(NOW);
        assertThat(result.getStatus())
                .isEqualTo(MonthlyClosingExecutionStatus.PROCESSING);
        assertThat(closing.getStatus())
                .isEqualTo(MonthlyClosingStatus.PROCESSING);

        ArgumentCaptor<MonthlyClosingItem> itemCaptor =
                ArgumentCaptor.forClass(MonthlyClosingItem.class);
        verify(itemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getOutputCode())
                .isEqualTo("MONTHLY_PAY_SLIP");
        assertThat(itemCaptor.getValue().getStatus())
                .isEqualTo(MonthlyClosingItemStatus.WAITING);
    }

    @Test
    void complete_shouldRejectIncompleteRequiredItem() {
        MonthlyClosingExecution execution =
                new MonthlyClosingExecution();
        execution.setId(10L);
        execution.setMonthlyClosingId(1L);
        execution.setClosingVersion(2);
        execution.setExecutedBy("sys-admin");
        when(executionRepository.findById(10L))
                .thenReturn(Optional.of(execution));

        MonthlyClosingItem item = new MonthlyClosingItem();
        item.setRequiredFlag(true);
        item.setStatus(MonthlyClosingItemStatus.FAILED);
        when(itemRepository
                .findByMonthlyClosingExecutionIdAndDeletedAtIsNullOrderByIdAsc(
                        10L
                ))
                .thenReturn(List.of(item));

        assertThatThrownBy(() -> service.complete(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("必須");
    }
}
