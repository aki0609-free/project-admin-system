package com.project.backend.features.customer.dto;

import java.util.List;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.features.customer.enums.CustomerInvoiceType;

public record CustomerSaveRequest(
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

        DayRule closingDayRule,
        DayRule paymentDayRule,

        List<CustomerSiteRequest> sites,
        List<CustomerEmployeeRequest> employees
) {
    public CustomerSaveRequest {
        sites = sites == null
                ? List.of()
                : List.copyOf(sites);

        employees = employees == null
                ? List.of()
                : List.copyOf(employees);
    }
}