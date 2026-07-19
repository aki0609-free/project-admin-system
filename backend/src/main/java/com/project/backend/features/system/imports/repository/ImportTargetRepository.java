package com.project.backend.features.system.imports.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.imports.entity.ImportTarget;

public interface ImportTargetRepository extends JpaRepository<ImportTarget, Long> {

    @EntityGraph(attributePaths = { "columns" })
    List<ImportTarget> findAllByDeletedAtIsNullOrderByIdAsc();

    @EntityGraph(attributePaths = { "columns" })
    List<ImportTarget> findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();

    @EntityGraph(attributePaths = { "columns" })
    Optional<ImportTarget> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = { "columns" })
    Optional<ImportTarget> findByTargetCodeAndActiveFlagTrueAndDeletedAtIsNull(String targetCode);

    boolean existsByTargetCodeAndDeletedAtIsNull(String targetCode);

    boolean existsByTargetCodeAndIdNotAndDeletedAtIsNull(String targetCode, Long id);
}