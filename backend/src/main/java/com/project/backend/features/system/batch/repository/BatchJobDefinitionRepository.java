package com.project.backend.features.system.batch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.batch.entity.BatchJobDefinition;

public interface BatchJobDefinitionRepository extends JpaRepository<BatchJobDefinition, Long> {

    List<BatchJobDefinition> findAllByDeletedAtIsNullOrderByIdAsc();

    List<BatchJobDefinition> findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();

    List<BatchJobDefinition> findByScheduleEnabledTrueAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();

    Optional<BatchJobDefinition> findByIdAndDeletedAtIsNull(Long id);

    Optional<BatchJobDefinition> findByJobCodeAndActiveFlagTrueAndDeletedAtIsNull(String jobCode);

    boolean existsByJobCodeAndDeletedAtIsNull(String jobCode);

    boolean existsByJobCodeAndIdNotAndDeletedAtIsNull(String jobCode, Long id);
}