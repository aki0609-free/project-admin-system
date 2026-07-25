package com.project.backend.features.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dashboard.dto.NoticeSaveRequest;
import com.project.backend.features.dashboard.entity.Notice;
import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.dashboard.enums.NoticeSourceType;
import com.project.backend.features.dashboard.enums.NoticeType;
import com.project.backend.features.dashboard.exception.NoticeConflictException;
import com.project.backend.features.dashboard.mapper.NoticeMapper;
import com.project.backend.features.dashboard.repository.NoticeRepository;
import com.project.backend.features.dashboard.service.renderer.NoticeContentRenderer;
import com.project.backend.features.dashboard.service.validation.NoticeAccessPolicy;
import com.project.backend.features.dashboard.service.validation.NoticeValidator;

class NoticeCommandServiceTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-07-31T15:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_INSTANT,
            ZoneId.of("Asia/Tokyo")
    );

    private NoticeRepository repository;
    private NoticeCommandService service;

    @BeforeEach
    void setUp() {
        repository = mock(NoticeRepository.class);
        when(repository.save(any(Notice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new NoticeCommandService(
                repository,
                mock(NoticeContentRenderer.class),
                new NoticeValidator(),
                new NoticeAccessPolicy(),
                mock(NoticeMapper.class),
                FIXED_CLOCK
        );
        TenantContext.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_shouldForceTenantAndManualSource() {
        service.create(request());

        org.mockito.ArgumentCaptor<Notice> captor =
                org.mockito.ArgumentCaptor.forClass(Notice.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getTenantId())
                .isEqualTo("tenant-a");
        assertThat(captor.getValue().getSourceType())
                .isEqualTo(NoticeSourceType.MANUAL);
        assertThat(captor.getValue().getSourceRuleCode()).isNull();
    }

    @Test
    void update_shouldRejectAutoNotice() {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setSourceType(NoticeSourceType.AUTO);

        when(repository.findByIdAndTenantIdAndDeletedAtIsNull(
                1L,
                "tenant-a"
        )).thenReturn(Optional.of(notice));

        assertThatThrownBy(() -> service.update(1L, request()))
                .isInstanceOf(NoticeConflictException.class);

        verify(repository, never()).save(notice);
    }

    @Test
    void delete_shouldUseApplicationClock() {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setSourceType(NoticeSourceType.MANUAL);
        notice.setActiveFlag(true);

        when(repository.findByIdAndTenantIdAndDeletedAtIsNull(
                1L,
                "tenant-a"
        )).thenReturn(Optional.of(notice));

        service.delete(1L);

        assertThat(notice.getDeletedAt())
                .isEqualTo(FIXED_INSTANT);
        assertThat(notice.isActiveFlag()).isFalse();
    }

    private NoticeSaveRequest request() {
        LocalDate today = LocalDate.of(2026, 7, 25);
        return new NoticeSaveRequest(
                " お知らせ ",
                today,
                today,
                NoticeType.INFO,
                "blue",
                NoticeContentFormat.PLAIN_TEXT,
                "本文",
                false,
                true
        );
    }
}
