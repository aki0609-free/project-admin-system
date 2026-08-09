package com.project.backend.features.tax.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.tax.entity.ResidentTaxInputRow;

public interface ResidentTaxInputRowRepository extends JpaRepository<ResidentTaxInputRow, Long> {
    List<ResidentTaxInputRow> findByBatchIdAndDeletedAtIsNullOrderByEmployeeIdAscMonthAsc(Long batchId);
    void deleteByBatchId(Long batchId);
}
