package com.project.backend.features.operation.preparation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.preparation.entity.DailyPreparationDispatch;

public interface DailyPreparationDispatchRepository
        extends JpaRepository<DailyPreparationDispatch, Long> {

    List<DailyPreparationDispatch> findByPreparationIdAndDeletedAtIsNullOrderByCustomerNameAscSiteNameAscIdAsc(
            Long preparationId
    );

    Optional<DailyPreparationDispatch> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByPreparationIdAndCustomerSiteIdAndDeletedAtIsNull(
            Long preparationId,
            Long customerSiteId
    );

    boolean existsByPreparationIdAndCustomerSiteIdAndIdNotAndDeletedAtIsNull(
            Long preparationId,
            Long customerSiteId,
            Long id
    );

    boolean existsByCustomerIdAndDeletedAtIsNull(Long customerId);

    boolean existsByCustomerSiteIdAndDeletedAtIsNull(Long customerSiteId);
}
