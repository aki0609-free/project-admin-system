package com.project.backend.features.master.payrollitem.balance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollItemParameterDefinitionRepository
        extends JpaRepository<PayrollItemParameterDefinition, Long> {

    List<PayrollItemParameterDefinition>
            findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                    String tenantId,
                    Long balancePolicyId
            );

    List<PayrollItemParameterDefinition>
            findAllByTenantIdAndBalancePolicyIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                    String tenantId,
                    Long balancePolicyId
            );

    List<PayrollItemParameterDefinition>
            findAllByTenantIdAndBalancePolicyIdOrderByDisplayOrderAscIdAsc(
                    String tenantId,
                    Long balancePolicyId
            );

    Optional<PayrollItemParameterDefinition>
            findByTenantIdAndBalancePolicyIdAndParameterKeyAndActiveFlagTrueAndDeletedAtIsNull(
                    String tenantId,
                    Long balancePolicyId,
                    String parameterKey
            );
}
