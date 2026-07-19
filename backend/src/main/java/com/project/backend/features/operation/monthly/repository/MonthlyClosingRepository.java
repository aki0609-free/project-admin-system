package com.project.backend.features.operation.monthly.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.MonthlyClosing;

public interface MonthlyClosingRepository extends JpaRepository<MonthlyClosing, Long> {

    Optional<MonthlyClosing> findByTargetMonthAndDeletedAtIsNull(LocalDate targetMonth);

    boolean existsByTargetMonthAndDeletedAtIsNull(LocalDate targetMonth);
}