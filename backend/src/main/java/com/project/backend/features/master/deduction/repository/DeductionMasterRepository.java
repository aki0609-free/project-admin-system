package com.project.backend.features.master.deduction.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionUnit;

public interface DeductionMasterRepository extends JpaRepository<DeductionMaster, Long> {
    List<DeductionMaster> findByTenantIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(String tenantId);
    Optional<DeductionMaster> findByIdAndTenantIdAndDeletedAtIsNull(Long id, String tenantId);
    Optional<DeductionMaster> findByTenantIdAndDeductionCodeAndDeletedAtIsNull(
            String tenantId,
            String deductionCode
    );
    boolean existsByTenantIdAndDeductionCodeAndDeletedAtIsNull(
            String tenantId,
            String deductionCode
    );
    boolean existsByRuleNameAndDeletedAtIsNull(String ruleName);
    List<DeductionMaster> findByTenantIdAndShowOnDailyStatementTrueAndEnabledTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
            String tenantId
    );
    List<DeductionMaster> findByTenantIdAndDeductionUnitInAndShowOnDailyStatementTrueAndEnabledTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
            String tenantId,
            List<DeductionUnit> deductionUnits
    );
    List<DeductionMaster> findByTenantIdAndShowOnMonthlyStatementTrueAndEnabledTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
            String tenantId
    );
}
