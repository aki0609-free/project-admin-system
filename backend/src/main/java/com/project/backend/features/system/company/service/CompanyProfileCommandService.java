package com.project.backend.features.system.company.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.company.dto.CompanyProfileResponse;
import com.project.backend.features.system.company.dto.CompanyProfileSaveRequest;
import com.project.backend.features.system.company.entity.CompanyProfile;
import com.project.backend.features.system.company.mapper.CompanyProfileMapper;
import com.project.backend.features.system.company.repository.CompanyProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyProfileCommandService {

    private static final String DEFAULT_COMPANY_CODE =
            "DEFAULT";

    private final CompanyProfileRepository repository;
    private final CompanyProfileMapper mapper;

    @SuppressWarnings("null")
    public CompanyProfileResponse save(
            CompanyProfileSaveRequest request
    ) {
        validateRequest(request);

        String companyCode =
                normalizeCompanyCode(
                        request.companyCode()
                );

        CompanyProfile entity = repository
                .findByCompanyCodeAndDeletedAtIsNull(
                        companyCode
                )
                .orElseGet(CompanyProfile::new);

        validateDuplicateCompanyCode(
                entity,
                companyCode
        );

        CompanyProfileSaveRequest normalizedRequest =
                copyWithCompanyCode(
                        request,
                        companyCode
                );

        mapper.apply(
                entity,
                normalizedRequest
        );

        CompanyProfile saved =
                repository.save(entity);

        return mapper.toResponse(saved);
    }

    private void validateRequest(
            CompanyProfileSaveRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "会社情報は必須です。"
            );
        }

        if (!StringUtils.hasText(request.companyName())) {
            throw new IllegalArgumentException(
                    "会社名は必須です。"
            );
        }

        if (request.capitalAmount() != null
                && request.capitalAmount().signum() < 0) {
            throw new IllegalArgumentException(
                    "資本金は0以上で指定してください。"
            );
        }
    }

    private String normalizeCompanyCode(
            String companyCode
    ) {
        if (!StringUtils.hasText(companyCode)) {
            return DEFAULT_COMPANY_CODE;
        }

        return companyCode.trim();
    }

    private void validateDuplicateCompanyCode(
            CompanyProfile entity,
            String companyCode
    ) {
        boolean duplicated;

        if (entity.getId() == null) {
            duplicated = repository
                    .existsByCompanyCodeAndDeletedAtIsNull(
                            companyCode
                    );
        } else {
            duplicated = repository
                    .existsByCompanyCodeAndIdNotAndDeletedAtIsNull(
                            companyCode,
                            entity.getId()
                    );
        }

        if (duplicated) {
            throw new IllegalArgumentException(
                    "同じ会社コードが既に登録されています。"
                            + " companyCode="
                            + companyCode
            );
        }
    }

    private CompanyProfileSaveRequest copyWithCompanyCode(
            CompanyProfileSaveRequest request,
            String companyCode
    ) {
        return new CompanyProfileSaveRequest(
                companyCode,

                request.companyName(),
                request.companyNameKana(),
                request.shortName(),

                request.representativeTitle(),
                request.representativeName(),

                request.postalCode(),
                request.prefecture(),
                request.city(),
                request.addressLine1(),
                request.addressLine2(),

                request.phone(),
                request.fax(),
                request.email(),
                request.websiteUrl(),

                request.capitalAmount(),

                request.permitNumber(),
                request.qualifiedInvoiceIssuerNumber(),

                request.serviceArea(),

                request.businessContents(),
                request.certificationInformation(),

                request.invoiceBankName(),
                request.invoiceBankBranchName(),
                request.invoiceBankAccountType(),
                request.invoiceBankAccountNumber(),
                request.invoiceBankAccountHolder(),

                request.invoiceNote(),

                request.activeFlag()
        );
    }
}