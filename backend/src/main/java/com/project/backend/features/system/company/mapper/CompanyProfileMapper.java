package com.project.backend.features.system.company.mapper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.company.dto.CompanyProfileResponse;
import com.project.backend.features.system.company.dto.CompanyProfileSaveRequest;
import com.project.backend.features.system.company.entity.CompanyProfile;

@Component
public class CompanyProfileMapper {

    public CompanyProfile toEntity(
            CompanyProfileSaveRequest request
    ) {
        CompanyProfile entity = new CompanyProfile();
        apply(entity, request);
        return entity;
    }

    public void apply(
            CompanyProfile entity,
            CompanyProfileSaveRequest request
    ) {
        entity.setCompanyCode(
                normalizeRequired(
                        request.companyCode(),
                        "会社コード"
                )
        );

        entity.setCompanyName(
                normalizeRequired(
                        request.companyName(),
                        "会社名"
                )
        );

        entity.setCompanyNameKana(
                normalizeNullable(request.companyNameKana())
        );

        entity.setShortName(
                normalizeNullable(request.shortName())
        );

        entity.setRepresentativeTitle(
                normalizeNullable(request.representativeTitle())
        );

        entity.setRepresentativeName(
                normalizeNullable(request.representativeName())
        );

        entity.setPostalCode(
                normalizeNullable(request.postalCode())
        );

        entity.setPrefecture(
                normalizeNullable(request.prefecture())
        );

        entity.setCity(
                normalizeNullable(request.city())
        );

        entity.setAddressLine1(
                normalizeNullable(request.addressLine1())
        );

        entity.setAddressLine2(
                normalizeNullable(request.addressLine2())
        );

        entity.setPhone(
                normalizeNullable(request.phone())
        );

        entity.setFax(
                normalizeNullable(request.fax())
        );

        entity.setEmail(
                normalizeNullable(request.email())
        );

        entity.setWebsiteUrl(
                normalizeNullable(request.websiteUrl())
        );

        entity.setCapitalAmount(
                request.capitalAmount()
        );

        entity.setPermitNumber(
                normalizeNullable(request.permitNumber())
        );

        entity.setQualifiedInvoiceIssuerNumber(
                normalizeNullable(
                        request.qualifiedInvoiceIssuerNumber()
                )
        );

        entity.setServiceArea(
                normalizeNullable(request.serviceArea())
        );

        entity.setBusinessDescription(
                joinLines(request.businessContents())
        );

        entity.setCertificationInformation(
                joinLines(request.certificationInformation())
        );

        entity.setInvoiceBankName(
                normalizeNullable(request.invoiceBankName())
        );

        entity.setInvoiceBankBranchName(
                normalizeNullable(
                        request.invoiceBankBranchName()
                )
        );

        entity.setInvoiceBankAccountType(
                normalizeNullable(
                        request.invoiceBankAccountType()
                )
        );

        entity.setInvoiceBankAccountNumber(
                normalizeNullable(
                        request.invoiceBankAccountNumber()
                )
        );

        entity.setInvoiceBankAccountHolder(
                normalizeNullable(
                        request.invoiceBankAccountHolder()
                )
        );

        entity.setInvoiceNote(
                normalizeNullable(request.invoiceNote())
        );

        entity.setActiveFlag(
                request.activeFlag() != null
                        ? request.activeFlag()
                        : true
        );
    }

    public CompanyProfileResponse toResponse(
            CompanyProfile entity
    ) {
        String fullAddress = buildFullAddress(entity);

        return CompanyProfileResponse.builder()
                .id(entity.getId())

                .companyCode(entity.getCompanyCode())
                .companyName(entity.getCompanyName())
                .companyNameKana(entity.getCompanyNameKana())
                .shortName(entity.getShortName())

                .representativeTitle(
                        entity.getRepresentativeTitle()
                )
                .representativeName(
                        entity.getRepresentativeName()
                )
                .representativeDisplayName(
                        buildRepresentativeDisplayName(entity)
                )

                .postalCode(entity.getPostalCode())
                .prefecture(entity.getPrefecture())
                .city(entity.getCity())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())

                .fullAddress(fullAddress)
                .fullAddressWithPostalCode(
                        buildFullAddressWithPostalCode(
                                entity,
                                fullAddress
                        )
                )

                .phone(entity.getPhone())
                .fax(entity.getFax())
                .email(entity.getEmail())
                .websiteUrl(entity.getWebsiteUrl())

                .capitalAmount(entity.getCapitalAmount())

                .permitNumber(entity.getPermitNumber())
                .qualifiedInvoiceIssuerNumber(
                        entity.getQualifiedInvoiceIssuerNumber()
                )

                .serviceArea(entity.getServiceArea())

                .businessContents(
                        splitLines(
                                entity.getBusinessDescription()
                        )
                )

                .certificationInformation(
                        splitLines(
                                entity.getCertificationInformation()
                        )
                )

                .invoiceBankName(
                        entity.getInvoiceBankName()
                )
                .invoiceBankBranchName(
                        entity.getInvoiceBankBranchName()
                )
                .invoiceBankAccountType(
                        entity.getInvoiceBankAccountType()
                )
                .invoiceBankAccountNumber(
                        entity.getInvoiceBankAccountNumber()
                )
                .invoiceBankAccountHolder(
                        entity.getInvoiceBankAccountHolder()
                )

                .invoiceBankDisplayText(
                        buildInvoiceBankDisplayText(entity)
                )

                .invoiceNote(entity.getInvoiceNote())

                .activeFlag(
                        Boolean.TRUE.equals(entity.getActiveFlag())
                )
                .build();
    }

    @SuppressWarnings("null")
    private String buildRepresentativeDisplayName(
            CompanyProfile entity
    ) {
        return Stream.of(
                        entity.getRepresentativeTitle(),
                        entity.getRepresentativeName()
                )
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }

    @SuppressWarnings("null")
    private String buildFullAddress(
            CompanyProfile entity
    ) {
        return Stream.of(
                        entity.getPrefecture(),
                        entity.getCity(),
                        entity.getAddressLine1(),
                        entity.getAddressLine2()
                )
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining());
    }

    private String buildFullAddressWithPostalCode(
            CompanyProfile entity,
            String fullAddress
    ) {
        String postalCode =
                normalizeNullable(entity.getPostalCode());

        if (!StringUtils.hasText(postalCode)) {
            return fullAddress;
        }

        if (!StringUtils.hasText(fullAddress)) {
            return "〒" + postalCode;
        }

        return "〒"
                + postalCode
                + "\n"
                + fullAddress;
    }

    private String buildInvoiceBankDisplayText(
            CompanyProfile entity
    ) {
        @SuppressWarnings("null")
        String bankLine = Stream.of(
                        entity.getInvoiceBankName(),
                        entity.getInvoiceBankBranchName()
                )
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(" "));

        @SuppressWarnings("null")
        String accountLine = Stream.of(
                        entity.getInvoiceBankAccountType(),
                        entity.getInvoiceBankAccountNumber()
                )
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(" "));

        String holderLine =
                normalizeNullable(
                        entity.getInvoiceBankAccountHolder()
                );

        return Stream.of(
                        bankLine,
                        accountLine,
                        holderLine
                )
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    @SuppressWarnings("null")
    private List<String> splitLines(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }

        return Arrays.stream(
                        value.replace("\r\n", "\n")
                                .replace("\r", "\n")
                                .split("\n")
                )
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String joinLines(
            List<String> values
    ) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        @SuppressWarnings("null")
        String result = values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("\n"));

        return StringUtils.hasText(result)
                ? result
                : null;
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        String normalized =
                normalizeNullable(value);

        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(
                    fieldName + "は必須です。"
            );
        }

        return normalized;
    }

    private String normalizeNullable(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}