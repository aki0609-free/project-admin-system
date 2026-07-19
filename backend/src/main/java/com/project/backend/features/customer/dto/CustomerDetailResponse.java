package com.project.backend.features.customer.dto;

import java.util.List;

import com.project.backend.common.dayrule.dto.DayRuleResponse;
import com.project.backend.features.customer.enums.CustomerInvoiceType;

public record CustomerDetailResponse(
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
        String latestPaymentStatus,

        List<CustomerSiteResponse> sites,
        List<CustomerEmployeeResponse> employees
) {
}