package com.project.backend.features.system.imports.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.imports.dto.ImportTargetCatalogColumnResponse;
import com.project.backend.features.system.imports.dto.ImportTargetCatalogResponse;
import com.project.backend.features.system.imports.entity.ImportTargetCatalog;
import com.project.backend.features.system.imports.repository.ImportTargetCatalogRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportTargetCatalogService {

    private final ImportTargetCatalogRepository repository;

    @Transactional(readOnly = true)
    public List<ImportTargetCatalogResponse> findActive() {
        return repository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderByDisplayNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportTargetCatalog findRequired(String tableName) {
        return repository
                .findByTableNameAndActiveFlagTrueAndDeletedAtIsNull(
                        tableName
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "許可された取込先テーブルではありません。 tableName="
                                        + tableName
                        ));
    }

    private ImportTargetCatalogResponse toResponse(
            ImportTargetCatalog catalog
    ) {
        return new ImportTargetCatalogResponse(
                catalog.getTableName(),
                catalog.getDisplayName(),
                catalog.getDescription(),
                catalog.isTenantScopedFlag(),
                catalog.isAllowDeleteInsertFlag(),
                catalog.getColumns().stream()
                        .filter(column ->
                                column.getDeletedAt() == null
                                        && column.isActiveFlag())
                        .sorted(Comparator.comparingInt(
                                column -> column.getOrderNo()
                        ))
                        .map(column ->
                                new ImportTargetCatalogColumnResponse(
                                        column.getColumnName(),
                                        column.getDisplayName(),
                                        column.getDataType(),
                                        column.getOrderNo()
                                ))
                        .toList()
        );
    }
}
