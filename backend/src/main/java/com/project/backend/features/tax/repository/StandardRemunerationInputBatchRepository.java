package com.project.backend.features.tax.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.tax.entity.StandardRemunerationInputBatch;

public interface StandardRemunerationInputBatchRepository
        extends JpaRepository<StandardRemunerationInputBatch, Long> {

    Optional<StandardRemunerationInputBatch>
            findFirstByConfirmedAtIsNullAndDeletedAtIsNullOrderByIdDesc();
}
