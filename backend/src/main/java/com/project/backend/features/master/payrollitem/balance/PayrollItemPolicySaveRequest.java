package com.project.backend.features.master.payrollitem.balance;

import java.util.List;

import com.project.backend.features.master.payrollitem.enums.PayrollItemInputSource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PayrollItemPolicySaveRequest(
        @NotNull PayrollItemApplicationScope applicationScope,
        @NotNull PayrollItemInputSource inputSource,
        boolean balanceTracking,
        BalanceUnit balanceUnit,
        String accrualFrequency,
        String accrualRuleName,
        boolean carryForward,
        boolean advanceConsumption,
        @Valid List<PayrollItemParameterDefinitionSaveRequest> parameterDefinitions
) {
}
