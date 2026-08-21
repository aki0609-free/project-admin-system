package com.project.backend.features.master.payrollitem.balance;

import java.util.List;

import com.project.backend.features.employee.dto.PayrollItemParameterDefinitionResponse;

public record PayrollItemPolicyResponse(
        String applicationScope,
        String inputSource,
        boolean balanceTracking,
        String balanceUnit,
        String accrualFrequency,
        String accrualRuleName,
        boolean carryForward,
        boolean advanceConsumption,
        List<PayrollItemParameterDefinitionResponse> parameterDefinitions
) {
}
