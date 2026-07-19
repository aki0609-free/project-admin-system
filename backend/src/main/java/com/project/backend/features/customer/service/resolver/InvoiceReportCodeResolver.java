package com.project.backend.features.customer.service.resolver;

import org.springframework.stereotype.Component;

import com.project.backend.features.customer.enums.CustomerInvoiceType;

@Component
public class InvoiceReportCodeResolver {

    public String resolve(
            CustomerInvoiceType invoiceType
    ) {
        CustomerInvoiceType normalized =
                invoiceType != null
                        ? invoiceType
                        : CustomerInvoiceType.PATTERN_1;

        return switch (normalized) {
            case PATTERN_1 ->
                    "MONTHLY_INVOICE_PATTERN_1";

            case PATTERN_2 ->
                    "MONTHLY_INVOICE_PATTERN_2";

            case PATTERN_3 ->
                    "MONTHLY_INVOICE_PATTERN_3";
        };
    }
}