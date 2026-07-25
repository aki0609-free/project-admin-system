package com.project.backend.features.master.allowance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.master.allowance.entity.AllowanceMaster;
import com.project.backend.features.master.allowance.enums.AllowanceUnit;

public interface AllowanceMasterRepository extends JpaRepository<AllowanceMaster, Long> {
    List<AllowanceMaster> findByTenantIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(String tenantId);
    Optional<AllowanceMaster> findByIdAndTenantIdAndDeletedAtIsNull(Long id, String tenantId);
    Optional<AllowanceMaster> findByTenantIdAndAllowanceCodeAndDeletedAtIsNull(
            String tenantId,
            String allowanceCode
    );
    boolean existsByTenantIdAndAllowanceCodeAndDeletedAtIsNull(
            String tenantId,
            String allowanceCode
    );
    boolean existsByRuleNameAndDeletedAtIsNull(String ruleName);
    List<AllowanceMaster> findByTenantIdAndShowOnDailyStatementTrueAndEnabledTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
            String tenantId
    );
    List<AllowanceMaster> findByTenantIdAndAllowanceUnitInAndShowOnDailyStatementTrueAndEnabledTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
            String tenantId,
            List<AllowanceUnit> allowanceUnits
    );
    List<AllowanceMaster> findByTenantIdAndShowOnMonthlyStatementTrueAndEnabledTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
            String tenantId
    );
}
