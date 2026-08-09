package com.project.backend.features.tax.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.tax.entity.ResidentTaxInputBatch;

public interface ResidentTaxInputBatchRepository extends JpaRepository<ResidentTaxInputBatch, Long> {
    Optional<ResidentTaxInputBatch> findFirstByFiscalYearAndSourceTypeAndConfirmedAtIsNullOrderByIdDesc(
            Integer fiscalYear, String sourceType);
}
