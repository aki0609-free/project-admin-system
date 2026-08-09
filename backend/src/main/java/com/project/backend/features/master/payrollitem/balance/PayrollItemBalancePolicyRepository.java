package com.project.backend.features.master.payrollitem.balance;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public interface PayrollItemBalancePolicyRepository
        extends JpaRepository<PayrollItemBalancePolicy, Long> {

    Optional<PayrollItemBalancePolicy>
            findByTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                    PayrollItemTargetType targetType,
                    Long targetMasterId
            );

    Optional<PayrollItemBalancePolicy>
            findByTargetTypeAndTargetCodeAndDeletedAtIsNull(
                    PayrollItemTargetType targetType,
                    String targetCode
            );

    List<PayrollItemBalancePolicy> findAllByDeletedAtIsNullOrderByIdAsc();
}
