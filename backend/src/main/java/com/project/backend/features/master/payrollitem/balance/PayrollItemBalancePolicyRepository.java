package com.project.backend.features.master.payrollitem.balance;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public interface PayrollItemBalancePolicyRepository
        extends JpaRepository<PayrollItemBalancePolicy, Long> {

    Optional<PayrollItemBalancePolicy>
            findByTenantIdAndTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                    String tenantId,
                    PayrollItemTargetType targetType,
                    Long targetMasterId
            );

    Optional<PayrollItemBalancePolicy>
            findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                    String tenantId,
                    PayrollItemTargetType targetType,
                    String targetCode
            );

    List<PayrollItemBalancePolicy>
            findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc(String tenantId);
}
