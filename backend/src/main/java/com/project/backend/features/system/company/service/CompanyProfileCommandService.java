package com.project.backend.features.system.company.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.app.tenant.context.TenantContext;
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
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );
    private static final Pattern INVOICE_ISSUER_PATTERN = Pattern.compile(
            "^T\\d{13}$"
    );

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
        String tenantId = requireTenantId();

        CompanyProfile entity = repository
                .findFirstByTenantIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                        tenantId
                )
                .or(() -> repository
                        .findByTenantIdAndCompanyCodeAndDeletedAtIsNull(
                                tenantId,
                                companyCode
                        ))
                .orElseGet(CompanyProfile::new);

        validateDuplicateCompanyCode(
                entity,
                tenantId,
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
        entity.setTenantId(tenantId);
        entity.setActiveFlag(true);

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

        validateMaxLength(request.companyCode(), 100, "会社コード");
        validateMaxLength(request.companyName(), 255, "会社名");
        validateMaxLength(request.companyNameKana(), 255, "会社名カナ");
        validateMaxLength(request.shortName(), 100, "略称");
        validateMaxLength(request.representativeTitle(), 100, "代表者役職");
        validateMaxLength(request.representativeName(), 255, "代表者名");
        validateMaxLength(request.postalCode(), 20, "郵便番号");
        validateMaxLength(request.prefecture(), 100, "都道府県");
        validateMaxLength(request.city(), 100, "市区町村");
        validateMaxLength(request.addressLine1(), 255, "住所1");
        validateMaxLength(request.addressLine2(), 255, "住所2");
        validateMaxLength(request.phone(), 50, "電話番号");
        validateMaxLength(request.fax(), 50, "FAX");
        validateMaxLength(request.email(), 255, "メールアドレス");
        validateMaxLength(request.websiteUrl(), 500, "Webサイト");
        validateMaxLength(request.permitNumber(), 255, "代表許可番号");
        validateMaxLength(
                request.qualifiedInvoiceIssuerNumber(),
                50,
                "適格請求書発行事業者登録番号"
        );
        validateMaxLength(request.serviceArea(), 500, "対応エリア");
        validateMaxLength(request.invoiceBankName(), 255, "銀行名");
        validateMaxLength(request.invoiceBankBranchName(), 255, "支店名");
        validateMaxLength(request.invoiceBankAccountType(), 50, "口座種別");
        validateMaxLength(request.invoiceBankAccountNumber(), 100, "口座番号");
        validateMaxLength(request.invoiceBankAccountHolder(), 255, "口座名義");

        if (StringUtils.hasText(request.email())
                && !EMAIL_PATTERN.matcher(request.email().trim()).matches()) {
            throw new IllegalArgumentException(
                    "メールアドレスの形式が正しくありません。"
            );
        }

        if (StringUtils.hasText(request.websiteUrl())) {
            validateWebsiteUrl(request.websiteUrl().trim());
        }

        if (StringUtils.hasText(request.qualifiedInvoiceIssuerNumber())
                && !INVOICE_ISSUER_PATTERN.matcher(
                        request.qualifiedInvoiceIssuerNumber().trim()
                ).matches()) {
            throw new IllegalArgumentException(
                    "適格請求書発行事業者登録番号はTと13桁の数字で入力してください。"
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
            String tenantId,
            String companyCode
    ) {
        boolean duplicated;

        if (entity.getId() == null) {
            duplicated = repository
                    .existsByTenantIdAndCompanyCodeAndDeletedAtIsNull(
                            tenantId,
                            companyCode
                    );
        } else {
            duplicated = repository
                    .existsByTenantIdAndCompanyCodeAndIdNotAndDeletedAtIsNull(
                            tenantId,
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

    private void validateMaxLength(
            String value,
            int maxLength,
            String fieldName
    ) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "は" + maxLength + "文字以内で入力してください。"
            );
        }
    }

    private void validateWebsiteUrl(String value) {
        try {
            URI uri = new URI(value);
            if (!("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException(
                        "Webサイトはhttp://またはhttps://から入力してください。"
                );
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "WebサイトのURL形式が正しくありません。"
            );
        }
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("テナント情報が取得できません。");
        }
        return tenantId;
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
