package com.project.backend.features.customer.dto;

import java.util.List;
import java.math.BigDecimal;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.features.customer.enums.CustomerContractStatus;
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
        BigDecimal distanceBillingUnitPrice,
        CustomerContractStatus contractFlag,

        CustomerInvoiceType invoiceType,

        DayRule closingDayRule,
        DayRule paymentDayRule,

        List<CustomerSiteRequest> sites,
        List<CustomerEmployeeRequest> employees
) {
    /** 既存のJava呼び出し互換。未指定時は30円/km。 */
    public CustomerSaveRequest(
            String name,
            String furiganaName,
            String shortName,
            String postNo,
            String address,
            String representativeName,
            String phone,
            String jobType,
            CustomerContractStatus contractFlag,
            CustomerInvoiceType invoiceType,
            DayRule closingDayRule,
            DayRule paymentDayRule,
            List<CustomerSiteRequest> sites,
            List<CustomerEmployeeRequest> employees
    ) {
        this(name, furiganaName, shortName, postNo, address,
                representativeName, phone, jobType,
                new BigDecimal("30.00"), contractFlag, invoiceType,
                closingDayRule, paymentDayRule, sites, employees);
    }

    public CustomerSaveRequest {
        sites = sites == null
                ? List.of()
                : List.copyOf(sites);

        employees = employees == null
                ? List.of()
                : List.copyOf(employees);
    }
}
