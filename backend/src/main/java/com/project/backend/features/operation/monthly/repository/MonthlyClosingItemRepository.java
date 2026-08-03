package com.project.backend.features.operation.monthly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.MonthlyClosingItem;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;

public interface MonthlyClosingItemRepository
        extends JpaRepository<MonthlyClosingItem, Long> {

    List<MonthlyClosingItem>
            findByMonthlyClosingExecutionIdAndDeletedAtIsNullOrderByIdAsc(
                    Long monthlyClosingExecutionId
            );

    Optional<MonthlyClosingItem>
            findByMonthlyClosingExecutionIdAndOutputTypeAndOutputCodeAndTargetKeyAndDeletedAtIsNull(
                    Long monthlyClosingExecutionId,
                    MonthlyClosingOutputType outputType,
                    String outputCode,
                    String targetKey
            );
}
