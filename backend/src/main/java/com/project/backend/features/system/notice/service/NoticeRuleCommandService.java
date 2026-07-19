package com.project.backend.features.system.notice.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.notice.dto.NoticeRuleResponse;
import com.project.backend.features.system.notice.dto.NoticeRuleSaveRequest;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.mapper.NoticeRuleMapper;
import com.project.backend.features.system.notice.repository.NoticeRuleRepository;
import com.project.backend.features.system.notice.service.validation.NoticeRuleValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeRuleCommandService {

    private final NoticeRuleRepository repository;
    private final NoticeRuleMapper mapper;
    private final NoticeRuleValidator validator;
    private final NoticeDynamicSchedulerService schedulerService;

    @Transactional
    public NoticeRuleResponse create(
            NoticeRuleSaveRequest request
    ) {
        validator.validateForCreate(request);

        NoticeRule entity = new NoticeRule();

        mapper.applyRequest(
                request,
                entity
        );

        NoticeRule saved =
                repository.save(entity);

        schedulerService.reloadRule(
                saved.getId()
        );

        return mapper.toResponse(saved);
    }

    @SuppressWarnings("null")
    @Transactional
    public NoticeRuleResponse update(
            Long id,
            NoticeRuleSaveRequest request
    ) {
        validator.validateForUpdate(
                id,
                request
        );

        NoticeRule entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "NoticeRuleが見つかりません。 id=" + id
                                )
                        );

        mapper.applyRequest(
                request,
                entity
        );

        NoticeRule saved =
                repository.save(entity);

        schedulerService.reloadRule(
                saved.getId()
        );

        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(
            Long id
    ) {
        NoticeRule entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "NoticeRuleが見つかりません。 id=" + id
                                )
                        );

        entity.setDeletedAt(
                Instant.now()
        );

        schedulerService.cancel(id);
    }
}