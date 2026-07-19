package com.project.backend.features.system.company.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;

@Builder
public record CompanyProfileResponse(

        Long id,

        String companyCode,
        String companyName,
        String companyNameKana,
        String shortName,

        String representativeTitle,
        String representativeName,
        String representativeDisplayName,

        String postalCode,
        String prefecture,
        String city,
        String addressLine1,
        String addressLine2,

        String fullAddress,
        String fullAddressWithPostalCode,

        String phone,
        String fax,
        String email,
        String websiteUrl,

        BigDecimal capitalAmount,

        String permitNumber,
        String qualifiedInvoiceIssuerNumber,

        String serviceArea,

        List<String> businessContents,
        List<String> certificationInformation,

        String invoiceBankName,
        String invoiceBankBranchName,
        String invoiceBankAccountType,
        String invoiceBankAccountNumber,
        String invoiceBankAccountHolder,

        String invoiceBankDisplayText,
        String invoiceNote,

        Boolean activeFlag

) {
}