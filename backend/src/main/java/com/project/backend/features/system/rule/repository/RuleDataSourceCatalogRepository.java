package com.project.backend.features.system.rule.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.rule.entity.RuleDataSourceCatalog;

public interface RuleDataSourceCatalogRepository
        extends JpaRepository<RuleDataSourceCatalog, Long> {

    List<RuleDataSourceCatalog>
            findByActiveFlagTrueAndDeletedAtIsNullOrderBySourceCodeAsc();

    Optional<RuleDataSourceCatalog>
            findBySourceCodeAndActiveFlagTrueAndDeletedAtIsNull(
                    String sourceCode
            );
}
