package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.mapper.MonthlyClosingMapper;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;

class MonthlyClosingCommandServiceTest {

    private MonthlyClosingRepository repository;
    private MonthlyClosingJobService jobService;
    private MonthlyClosingPeriodService periodService;
    private MonthlyClosingCommandService service;
    private MonthlyClosing entity;
    private MonthlyClosingPeriod period;

    @BeforeEach
    void setUp() {
        repository = mock(MonthlyClosingRepository.class);
        jobService = mock(MonthlyClosingJobService.class);
        periodService = mock(MonthlyClosingPeriodService.class);
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

        when(repository.findByTargetMonthAndDeletedAtIsNull(
                LocalDate.of(2026, 7, 1)
        )).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(periodService.resolve("2026-07")).thenReturn(period);

        service = new MonthlyClosingCommandService(
                repository,
                mock(MonthlyClosingMapper.class),
                jobService,
                periodService,
                Clock.fixed(
                        Instant.parse("2026-08-01T00:00:00Z"),
                        ZoneId.of("Asia/Tokyo")
                )
        );
    }

    @Test
    void close_shouldMarkClosedOnlyAfterClosingJobsComplete() {
        service.close("2026-07");

        verify(jobService).executeClosing(10L, period, 1);
        assertThat(entity.getStatus())
                .isEqualTo(MonthlyClosingStatus.CLOSED);
        assertThat(entity.getClosingVersion()).isEqualTo(1);
        assertThat(entity.getClosedAt())
                .isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
    }

    @Test
    void close_shouldNotMarkClosedWhenReportGenerationFails() {
        doThrow(new IllegalStateException("帳票生成失敗"))
                .when(jobService)
                .executeClosing(10L, period, 1);

        assertThatThrownBy(() -> service.close("2026-07"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("帳票生成失敗");
        assertThat(entity.getStatus())
                .isNotEqualTo(MonthlyClosingStatus.CLOSED);
        assertThat(entity.getClosingVersion()).isZero();
    }
}
