package com.project.backend.features.system.batch.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.batch.dto.BatchJobDefinitionResponse;
import com.project.backend.features.system.batch.mapper.BatchJobMapper;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchJobDefinitionQueryService {

    private final BatchJobDefinitionRepository repository;
    private final BatchJobMapper mapper;
    private final BatchJobDefinitionLookupService lookupService;

    public List<BatchJobDefinitionResponse> findAll() {
        return mapper.toDefinitionResponseList(
                repository.findAllByDeletedAtIsNullOrderByIdAsc()
        );
    }

    public BatchJobDefinitionResponse findDetail(Long id) {
        return mapper.toDefinitionResponse(
                lookupService.find(id)
        );
    }
}