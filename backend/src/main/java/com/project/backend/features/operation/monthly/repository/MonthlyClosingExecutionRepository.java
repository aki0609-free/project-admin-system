package com.project.backend.features.operation.monthly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.MonthlyClosingExecution;

public interface MonthlyClosingExecutionRepository
        extends JpaRepository<MonthlyClosingExecution, Long> {

    Optional<MonthlyClosingExecution>
            findByMonthlyClosingIdAndClosingVersionAndDeletedAtIsNull(
                    Long monthlyClosingId,
                    Integer closingVersion
            );

    List<MonthlyClosingExecution>
            findByMonthlyClosingIdAndDeletedAtIsNullOrderByClosingVersionDesc(
                    Long monthlyClosingId
            );
}
