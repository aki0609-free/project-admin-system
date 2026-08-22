package com.project.backend.features.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dashboard.entity.Notice;
import com.project.backend.features.dashboard.enums.NoticeType;
import com.project.backend.features.dashboard.mapper.NoticeMapper;
import com.project.backend.features.dashboard.repository.NoticeRepository;

class NoticeQueryServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-31T15:30:00Z"),
            ZoneId.of("Asia/Tokyo")
    );

    private NoticeRepository repository;
    private NoticeMapper mapper;
    private NoticeQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(NoticeRepository.class);
        mapper = mock(NoticeMapper.class);
        service = new NoticeQueryService(
                repository,
                mapper,
                FIXED_CLOCK
        );
        TenantContext.setTenantId("tenant-a");
    }

    @Test
    void findAll_shouldSortByPinnedDateDistanceEndDateAndType() {
        Notice oldImportant = notice(
                1L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                NoticeType.IMPORTANT,
                false
        );
        Notice nearInfo = notice(
                2L,
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 3),
                NoticeType.INFO,
                false
        );
        Notice pinnedWarning = notice(
                3L,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 1, 2),
                NoticeType.WARNING,
                true
        );

        when(repository.findAllActive(
                eq("tenant-a"),
                any(Pageable.class)
        )).thenReturn(List.of(oldImportant, nearInfo, pinnedWarning));

        service.findAll();

        var order = inOrder(mapper);
        order.verify(mapper).toResponse(pinnedWarning);
        order.verify(mapper).toResponse(nearInfo);
        order.verify(mapper).toResponse(oldImportant);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findAll_shouldQueryAllActiveNoticesForCurrentTenant() {
        when(repository.findAllActive(
                eq("tenant-a"),
                any(Pageable.class)
        )).thenReturn(List.of(new Notice()));

        assertThat(service.findAll()).hasSize(1);

        verify(repository).findAllActive(
                eq("tenant-a"),
                any(Pageable.class)
        );
    }

    @Test
    void findByPeriod_shouldUseOverlapQueryForCurrentTenant() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);

        when(repository.findActiveOverlappingPeriod(
                eq("tenant-a"),
                eq(from),
                eq(to),
                any(Pageable.class)
        )).thenReturn(List.of());

        service.findByPeriod(from, to);

        verify(repository).findActiveOverlappingPeriod(
                eq("tenant-a"),
                eq(from),
                eq(to),
                any(Pageable.class)
        );
    }

    @Test
    void findByPeriod_shouldRejectInvalidOrExcessiveRange() {
        assertThatThrownBy(() ->
                service.findByPeriod(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 7, 1)
                ))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
                service.findByPeriod(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2027, 1, 3)
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("366日以内");
    }

    private Notice notice(
            Long id,
            LocalDate startDate,
            LocalDate endDate,
            NoticeType type,
            boolean pinned
    ) {
        Notice notice = new Notice();
        notice.setId(id);
        notice.setStartDate(startDate);
        notice.setEndDate(endDate);
        notice.setType(type);
        notice.setPinnedFlag(pinned);
        return notice;
    }
}
