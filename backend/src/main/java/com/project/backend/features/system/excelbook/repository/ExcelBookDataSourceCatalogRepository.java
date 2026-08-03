package com.project.backend.features.system.excelbook.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalog;

public interface ExcelBookDataSourceCatalogRepository
        extends JpaRepository<ExcelBookDataSourceCatalog, Long> {

    @EntityGraph(attributePaths = "columns")
    List<ExcelBookDataSourceCatalog>
            findByActiveFlagTrueAndDeletedAtIsNullOrderByDisplayNameAsc();

    Optional<ExcelBookDataSourceCatalog>
            findBySourceCodeAndActiveFlagTrueAndDeletedAtIsNull(
                    String sourceCode
            );
}
