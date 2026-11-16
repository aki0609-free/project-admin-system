package com.project.backend.features.tax.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.tax.entity.StandardRemunerationInputRow;

public interface StandardRemunerationInputRowRepository
        extends JpaRepository<StandardRemunerationInputRow, Long> {

    List<StandardRemunerationInputRow>
            findByBatchIdAndDeletedAtIsNullOrderByEmployeeIdAscEffectiveFromAsc(Long batchId);

    void deleteByBatchId(Long batchId);
}
