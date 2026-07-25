package com.project.backend.features.system.rule.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.rule.dto.RuleMasterResponse;
import com.project.backend.features.system.rule.dto.RuleMasterSaveRequest;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.mapper.RuleMasterMapper;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;
import com.project.backend.features.system.rule.exception.RuleConflictException;
import com.project.backend.features.system.rule.service.validation.RuleReferenceChecker;
import com.project.backend.features.system.rule.service.validation.RuleMasterValidator;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleMasterCommandService {

    private final RuleMasterRepository repository;
    private final RuleMasterMapper mapper;
    private final RuleMasterValidator validator;
    private final RuleReferenceChecker referenceChecker;

    @Transactional
    public RuleMasterResponse create(RuleMasterSaveRequest request) {
        validator.validateForCreate(request);

        RuleMaster entity = new RuleMaster();
        mapper.applyRequest(entity, request);

        return mapper.toResponse(repository.save(entity));
    }

    @SuppressWarnings("null")
    @Transactional
    public RuleMasterResponse update(Long id, RuleMasterSaveRequest request) {
        RuleMaster entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Ruleが見つかりません。 id=" + id
                                )
                        );

        validator.validateForUpdate(entity, request);

        if (entity.isActiveFlag()
                && !request.activeFlag()
                && referenceChecker.isReferenced(entity.getRuleName())) {
            throw new RuleConflictException(
                    "参照中のRuleは無効化できません。 ruleName="
                            + entity.getRuleName()
            );
        }

        mapper.applyRequest(entity, request);

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        RuleMaster entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Ruleが見つかりません。 id=" + id
                                )
                        );

        var referenceTypes =
                referenceChecker.findReferenceTypes(
                        entity.getRuleName()
                );

        if (!referenceTypes.isEmpty()) {
            throw new RuleConflictException(
                    "参照中のRuleは削除できません。 ruleName="
                            + entity.getRuleName()
                            + ", references="
                            + referenceTypes
            );
        }

        Instant now = Instant.now();

        entity.setDeletedAt(now);

        if (entity.getParameters() != null) {
            entity.getParameters().forEach(parameter -> parameter.setDeletedAt(now));
        }

        if (entity.getDataSources() != null) {
            entity.getDataSources().forEach(dataSource -> {
                dataSource.setDeletedAt(now);

                if (dataSource.getColumns() != null) {
                    dataSource.getColumns().forEach(column -> column.setDeletedAt(now));
                }
            });
        }
    }
}
