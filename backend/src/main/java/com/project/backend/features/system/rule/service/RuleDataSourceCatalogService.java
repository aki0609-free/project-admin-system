package com.project.backend.features.system.rule.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.rule.dto.RuleDataSourceCatalogColumnResponse;
import com.project.backend.features.system.rule.dto.RuleDataSourceCatalogResponse;
import com.project.backend.features.system.rule.entity.RuleDataSourceCatalog;
import com.project.backend.features.system.rule.repository.RuleDataSourceCatalogRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleDataSourceCatalogService {

    private final RuleDataSourceCatalogRepository repository;

    @Transactional(readOnly = true)
    public List<RuleDataSourceCatalogResponse> findActive() {
        return repository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderBySourceCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RuleDataSourceCatalog findRequired(String sourceCode) {
        return repository
                .findBySourceCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        sourceCode
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Ruleデータソースカタログが見つかりません。 sourceCode="
                                        + sourceCode
                        ));
    }

    private RuleDataSourceCatalogResponse toResponse(
            RuleDataSourceCatalog catalog
    ) {
        return new RuleDataSourceCatalogResponse(
                catalog.getSourceCode(),
                catalog.getDisplayName(),
                catalog.getDescription(),
                catalog.isTenantScopedFlag(),
                catalog.getMaxRows(),
                catalog.getColumns().stream()
                        .filter(column ->
                                column.getDeletedAt() == null
                                        && column.isActiveFlag())
                        .sorted(Comparator.comparingInt(
                                column -> column.getOrderNo()
                        ))
                        .map(column ->
                                new RuleDataSourceCatalogColumnResponse(
                                        column.getColumnName(),
                                        column.getDisplayName(),
                                        column.getDataType(),
                                        column.getOrderNo()
                                ))
                        .toList()
        );
    }
}
