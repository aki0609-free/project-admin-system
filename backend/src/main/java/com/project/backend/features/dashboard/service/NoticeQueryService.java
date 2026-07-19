package com.project.backend.features.dashboard.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dashboard.dto.NoticeResponse;
import com.project.backend.features.dashboard.mapper.NoticeMapper;
import com.project.backend.features.dashboard.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeQueryService {

    private final NoticeRepository noticeRepository;
    private final NoticeMapper noticeMapper;

    @Transactional(readOnly = true)
    public List<NoticeResponse> findAll() {
        return noticeRepository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderByPinnedFlagDescStartDateDesc()
                .stream()
                .map(noticeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> findByPeriod(
            LocalDate from,
            LocalDate to
    ) {
        return noticeRepository
                .findByActiveFlagTrueAndDeletedAtIsNullAndStartDateBetweenOrderByStartDateAsc(
                        from,
                        to
                )
                .stream()
                .map(noticeMapper::toResponse)
                .toList();
    }
}