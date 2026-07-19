package com.project.backend.features.system.rule.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.rule.dto.RuleMasterResponse;
import com.project.backend.features.system.rule.dto.RuleMasterSaveRequest;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.mapper.RuleMasterMapper;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;
import com.project.backend.features.system.rule.service.validation.RuleMasterValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleMasterCommandService {

    private final RuleMasterRepository repository;
    private final RuleMasterMapper mapper;
    private final RuleMasterValidator validator;

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
        validator.validateForUpdate(id, request);

        RuleMaster entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException("Ruleが見つかりません。 id=" + id)
                        );

        mapper.applyRequest(entity, request);

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        RuleMaster entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException("Ruleが見つかりません。 id=" + id)
                        );

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