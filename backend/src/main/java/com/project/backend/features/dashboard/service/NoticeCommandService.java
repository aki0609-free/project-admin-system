package com.project.backend.features.dashboard.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.features.dashboard.dto.NoticeResponse;
import com.project.backend.features.dashboard.dto.NoticeSaveRequest;
import com.project.backend.features.dashboard.entity.Notice;
import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.dashboard.enums.NoticeSourceType;
import com.project.backend.features.dashboard.enums.NoticeType;
import com.project.backend.features.dashboard.mapper.NoticeMapper;
import com.project.backend.features.dashboard.repository.NoticeRepository;
import com.project.backend.features.dashboard.service.renderer.NoticeContentRenderer;
import com.project.backend.features.dashboard.service.validation.NoticeValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeCommandService {

    private final NoticeRepository noticeRepository;
    private final NoticeContentRenderer noticeContentRenderer;
    private final NoticeValidator noticeValidator;
    private final NoticeMapper noticeMapper;

    @Transactional
    public NoticeResponse create(NoticeSaveRequest request) {
        noticeValidator.validateSave(request);

        Notice notice = new Notice();

        applySaveRequest(
                notice,
                request
        );

        notice.setSourceType(NoticeSourceType.MANUAL);
        notice.setSourceRuleCode(null);

        return noticeMapper.toResponse(
                noticeRepository.save(notice)
        );
    }

    @SuppressWarnings("null")
    @Transactional
    public NoticeResponse update(
            Long id,
            NoticeSaveRequest request
    ) {
        noticeValidator.validateSave(request);

        Notice notice =
                noticeRepository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Noticeが見つかりません。 id=" + id
                                )
                        );

        applySaveRequest(
                notice,
                request
        );

        return noticeMapper.toResponse(
                noticeRepository.save(notice)
        );
    }

    @Transactional
    public void delete(Long id) {
        Notice notice =
                noticeRepository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Noticeが見つかりません。 id=" + id
                                )
                        );

        notice.setDeletedAt(Instant.now());
        notice.setActiveFlag(false);
    }

    private void applySaveRequest(
            Notice notice,
            NoticeSaveRequest request
    ) {
        NoticeContentFormat contentFormat =
                request.contentFormat() != null
                        ? request.contentFormat()
                        : NoticeContentFormat.PLAIN_TEXT;

        notice.setTitle(request.title());
        notice.setStartDate(request.start());
        notice.setEndDate(request.end());
        notice.setType(
                request.type() != null
                        ? request.type()
                        : NoticeType.INFO
        );
        notice.setColor(
                StringUtils.hasText(request.color())
                        ? request.color()
                        : "blue"
        );
        notice.setContentFormat(contentFormat);
        notice.setContent(
                noticeContentRenderer.render(
                        contentFormat,
                        request.content()
                )
        );
        notice.setPinnedFlag(Boolean.TRUE.equals(request.pinnedFlag()));
        notice.setActiveFlag(
                request.activeFlag() == null
                        || Boolean.TRUE.equals(request.activeFlag())
        );
    }
}