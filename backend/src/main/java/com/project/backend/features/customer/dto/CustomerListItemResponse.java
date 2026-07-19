package com.project.backend.features.customer.dto;

import com.project.backend.common.dayrule.dto.DayRuleResponse;
import com.project.backend.features.customer.enums.CustomerInvoiceType;

public record CustomerListItemResponse(
        Long id,
        String name,
        String furiganaName,
        String shortName,
        String postNo,
        String address,
        String representativeName,
        String phone,
        String jobType,
        String contractFlag,

        CustomerInvoiceType invoiceType,

        DayRuleResponse closingDayRule,
        DayRuleResponse paymentDayRule,

        Integer siteCount,
        Integer employeeCount,
        String latestPaymentStatus
) {
}