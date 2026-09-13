package com.project.backend.features.operation.monthly.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.MonthlyClosingExecution;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingExecutionStatus;

public interface MonthlyClosingExecutionRepository
        extends JpaRepository<MonthlyClosingExecution, Long> {

    boolean existsByMonthlyClosingIdAndClosingVersionAndStatusInAndDeletedAtIsNull(
            Long monthlyClosingId,
            Integer closingVersion,
            List<MonthlyClosingExecutionStatus> statuses
    );

    List<MonthlyClosingExecution>
            findByMonthlyClosingIdAndDeletedAtIsNullOrderByClosingVersionDesc(
                    Long monthlyClosingId
            );
}
