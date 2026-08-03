package com.project.backend.features.system.excelbook.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.excelbook.dto.ExcelBookDataSourceCatalogColumnResponse;
import com.project.backend.features.system.excelbook.dto.ExcelBookDataSourceCatalogResponse;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalog;
import com.project.backend.features.system.excelbook.repository.ExcelBookDataSourceCatalogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcelBookDataSourceCatalogService {

    private final ExcelBookDataSourceCatalogRepository repository;

    public List<ExcelBookDataSourceCatalogResponse> findActive() {
        return repository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderByDisplayNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ExcelBookDataSourceCatalog findRequired(String sourceCode) {
        return repository
                .findBySourceCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        sourceCode
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "許可された台帳データソースではありません: "
                                + sourceCode
                ));
    }

    private ExcelBookDataSourceCatalogResponse toResponse(
            ExcelBookDataSourceCatalog catalog
    ) {
        return new ExcelBookDataSourceCatalogResponse(
                catalog.getSourceCode(),
                catalog.getDisplayName(),
                catalog.getDescription(),
                catalog.getColumns().stream()
                        .filter(column ->
                                column.isActiveFlag()
                                        && column.getDeletedAt() == null
                        )
                        .map(column ->
                                new ExcelBookDataSourceCatalogColumnResponse(
                                        column.getColumnName(),
                                        column.getDisplayName(),
                                        column.getDataType(),
                                        column.getOrderNo()
                                )
                        )
                        .toList()
        );
    }
}
