package com.project.backend.features.system.imports.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.imports.entity.ImportTargetCatalog;

public interface ImportTargetCatalogRepository
        extends JpaRepository<ImportTargetCatalog, Long> {

    @EntityGraph(attributePaths = "columns")
    List<ImportTargetCatalog>
            findByActiveFlagTrueAndDeletedAtIsNullOrderByDisplayNameAsc();

    @EntityGraph(attributePaths = "columns")
    Optional<ImportTargetCatalog>
            findByTableNameAndActiveFlagTrueAndDeletedAtIsNull(
                    String tableName
            );
}
