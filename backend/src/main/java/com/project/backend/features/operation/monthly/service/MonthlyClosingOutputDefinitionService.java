package com.project.backend.features.operation.monthly.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyClosingOutputDefinitionService {

    private final MonthlyClosingOutputDefinitionRepository repository;

    public List<MonthlyClosingOutputDefinition> findActive() {
        return repository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc();
    }
}
