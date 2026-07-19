package com.project.backend.features.system.notice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.notice.dto.NoticeRuleResponse;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.mapper.NoticeRuleMapper;
import com.project.backend.features.system.notice.repository.NoticeRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeRuleQueryService {

    private final NoticeRuleRepository repository;
    private final NoticeRuleMapper mapper;

    @Transactional(readOnly = true)
    public List<NoticeRuleResponse> findAll() {
        return mapper.toResponseList(
                repository.findAllByDeletedAtIsNullOrderByIdAsc()
        );
    }

    @Transactional(readOnly = true)
    public NoticeRuleResponse findDetail(
            Long id
    ) {
        NoticeRule entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "NoticeRuleが見つかりません。 id=" + id
                                )
                        );

        return mapper.toResponse(entity);
    }
}