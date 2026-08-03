package com.project.backend.features.operation.monthly.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;

public interface MonthlyClosingOutputDefinitionRepository
        extends JpaRepository<MonthlyClosingOutputDefinition, Long> {

    List<MonthlyClosingOutputDefinition>
            findByActiveFlagTrueAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc();
}
