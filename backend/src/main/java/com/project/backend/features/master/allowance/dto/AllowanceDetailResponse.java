package com.project.backend.features.master.allowance.dto;

import java.util.List;
import java.util.Map;
import com.project.backend.features.master.payrollitem.balance.PayrollItemPolicyResponse;

public record AllowanceDetailResponse(
        Long id,
        String allowanceCode,
        String allowanceName,
        String allowanceType,
        String calculationType,
        String allowanceUnit,
        String detailViewType,
        String ruleName,
        Integer defaultAmount,
        Boolean allowManualInput,
        Integer minAmount,
        Integer maxAmount,
        Boolean taxable,
        Boolean showOnDailyStatement,
        Boolean showOnMonthlyStatement,
        Integer displayOrder,
        Boolean enabled,
        String note,
        Map<String, List<BaseAllowanceDetailResponse>> details,
        PayrollItemPolicyResponse policy
) {
}
