package com.project.backend.features.admin.business.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.admin.business.entity.EmployeeYearEndAdjustment;

public interface EmployeeYearEndAdjustmentRepository
        extends JpaRepository<EmployeeYearEndAdjustment, Long> {
    List<EmployeeYearEndAdjustment> findByTenantIdAndTaxYearAndDeletedAtIsNullOrderByEmployeeEmployeeCodeAsc(
            String tenantId, Integer taxYear);
    Optional<EmployeeYearEndAdjustment> findByIdAndTenantIdAndDeletedAtIsNull(Long id, String tenantId);
    Optional<EmployeeYearEndAdjustment> findByTenantIdAndEmployeeIdAndTaxYearAndDeletedAtIsNull(
            String tenantId, Long employeeId, Integer taxYear);
}
