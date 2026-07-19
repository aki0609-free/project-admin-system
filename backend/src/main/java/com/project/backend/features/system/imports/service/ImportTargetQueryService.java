package com.project.backend.features.system.imports.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.dto.ImportTargetSummary;
import com.project.backend.features.system.imports.mapper.ImportTargetMapper;
import com.project.backend.features.system.imports.repository.ImportTargetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImportTargetQueryService {

    private final ImportTargetRepository repository;
    private final ImportTargetLookupService lookupService;
    private final ImportTargetMapper mapper;

    public List<ImportTargetSummary> findAll() {
        return mapper.toSummaryList(
                repository.findAllByDeletedAtIsNullOrderByIdAsc()
        );
    }

    public List<ImportTargetSummary> findActiveTargets() {
        return mapper.toSummaryList(
                repository.findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc()
        );
    }

    public ImportTargetDefinition findDetail(Long id) {
        return mapper.toDefinition(
                lookupService.find(id)
        );
    }

    public ImportTargetDefinition findByTargetCode(String targetCode) {
        return mapper.toDefinition(
                lookupService.findActiveByTargetCode(targetCode)
        );
    }
}