package com.project.backend.features.operation.preparation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.preparation.entity.DailyPreparationAssignment;

public interface DailyPreparationAssignmentRepository
        extends JpaRepository<DailyPreparationAssignment, Long> {

    List<DailyPreparationAssignment> findByPreparationIdAndDeletedAtIsNullOrderByEmployeeCodeAscIdAsc(
            Long preparationId
    );

    Optional<DailyPreparationAssignment> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByPreparationIdAndEmployeeIdAndDeletedAtIsNull(
            Long preparationId,
            Long employeeId
    );

    Optional<DailyPreparationAssignment> findByPreparationIdAndEmployeeIdAndDeletedAtIsNull(
            Long preparationId,
            Long employeeId
    );

    boolean existsByPreparationIdAndEmployeeIdAndIdNotAndDeletedAtIsNull(
            Long preparationId,
            Long employeeId,
            Long id
    );

    boolean existsByEmployeeIdAndDeletedAtIsNull(Long employeeId);

    boolean existsByCustomerIdAndDeletedAtIsNull(Long customerId);

    boolean existsByCustomerSiteIdAndDeletedAtIsNull(Long customerSiteId);
}
