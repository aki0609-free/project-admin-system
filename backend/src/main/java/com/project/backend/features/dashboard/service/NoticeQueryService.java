package com.project.backend.features.dashboard.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dashboard.dto.NoticeResponse;
import com.project.backend.features.dashboard.mapper.NoticeMapper;
import com.project.backend.features.dashboard.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeQueryService {

    private static final int MAX_RESULT_COUNT = 1000;
    private static final long MAX_CALENDAR_RANGE_DAYS = 366;

    private final NoticeRepository noticeRepository;
    private final NoticeMapper noticeMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<NoticeResponse> findAll() {
        return noticeRepository
                .findCurrent(
                        requireTenantId(),
                        LocalDate.now(clock),
                        PageRequest.of(0, MAX_RESULT_COUNT)
                )
                .stream()
                .map(noticeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> findByPeriod(
            LocalDate from,
            LocalDate to
    ) {
        validatePeriod(from, to);

        return noticeRepository
                .findActiveOverlappingPeriod(
                        requireTenantId(),
                        from,
                        to,
                        PageRequest.of(0, MAX_RESULT_COUNT)
                )
                .stream()
                .map(noticeMapper::toResponse)
                .toList();
    }

    private void validatePeriod(
            LocalDate from,
            LocalDate to
    ) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("カレンダーの開始日と終了日は必須です。");
        }

        if (to.isBefore(from)) {
            throw new IllegalArgumentException("カレンダーの終了日は開始日以降にしてください。");
        }

        if (ChronoUnit.DAYS.between(from, to) > MAX_CALENDAR_RANGE_DAYS) {
            throw new IllegalArgumentException("カレンダーの取得期間は366日以内にしてください。");
        }
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("テナント情報が取得できません。");
        }
        return tenantId;
    }
}
