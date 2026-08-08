package com.project.backend.features.operation.monthly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;

public interface MonthlyClosingOutputDefinitionRepository
        extends JpaRepository<MonthlyClosingOutputDefinition, Long> {

    List<MonthlyClosingOutputDefinition>
            findByActiveFlagTrueAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc();

    List<MonthlyClosingOutputDefinition>
            findByOutputTypeAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                    MonthlyClosingOutputType outputType
            );

    Optional<MonthlyClosingOutputDefinition>
            findByOutputTypeAndOutputCodeAndDeletedAtIsNull(
                    MonthlyClosingOutputType outputType,
                    String outputCode
            );
}
