package com.project.backend.features.operation.preparation.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.preparation.entity.DailyPreparation;

public interface DailyPreparationRepository extends JpaRepository<DailyPreparation, Long> {

    Optional<DailyPreparation> findByTargetDateAndDeletedAtIsNull(LocalDate targetDate);

    Optional<DailyPreparation> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByTargetDateAndDeletedAtIsNull(LocalDate targetDate);
}