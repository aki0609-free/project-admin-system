package com.project.backend.features.system.rule.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.rule.dto.RuleMasterResponse;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.mapper.RuleMasterMapper;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleMasterQueryService {

    private final RuleMasterRepository repository;
    private final RuleMasterMapper mapper;

    @Transactional(readOnly = true)
    public List<RuleMasterResponse> findAll() {
        return mapper.toResponseList(
                repository.findAllByDeletedAtIsNullOrderByIdAsc()
        );
    }

    @Transactional(readOnly = true)
    public List<RuleMasterResponse> findActiveRules() {
        return mapper.toResponseList(
                repository.findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc()
        );
    }

    @Transactional(readOnly = true)
    public RuleMasterResponse findDetail(Long id) {
        RuleMaster entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException("Ruleが見つかりません。 id=" + id)
                        );

        return mapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public RuleMasterResponse findByRuleName(String ruleName) {
        RuleMaster entity =
                repository.findByRuleNameAndDeletedAtIsNull(ruleName)
                        .orElseThrow(() ->
                                new RuntimeException("Ruleが見つかりません。 ruleName=" + ruleName)
                        );

        return mapper.toResponse(entity);
    }
}