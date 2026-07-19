package com.project.backend.features.customer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.dto.CustomerSiteBillingRateRequest;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.entity.CustomerSiteBillingRate;
import com.project.backend.features.customer.mapper.CustomerSiteBillingRateMapper;
import com.project.backend.features.customer.repository.CustomerSiteBillingRateRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerSiteBillingRateCommandService {

    private final CustomerSiteRepository customerSiteRepository;
    private final CustomerSiteBillingRateRepository repository;
    private final CustomerSiteBillingRateMapper mapper;

    @SuppressWarnings("null")
    public Long create(
            Long customerSiteId,
            CustomerSiteBillingRateRequest request
    ) {
        validate(request);

        CustomerSite customerSite = findCustomerSite(customerSiteId);

        validateDuplicate(
                customerSiteId,
                null,
                request
        );

        CustomerSiteBillingRate entity =
                mapper.toEntity(customerSite, request);

        return repository.save(entity).getId();
    }

    @SuppressWarnings("null")
    public void update(
            Long customerSiteId,
            Long billingRateId,
            CustomerSiteBillingRateRequest request
    ) {
        validate(request);

        CustomerSiteBillingRate entity =
                findOwnedRate(customerSiteId, billingRateId);

        validateDuplicate(
                customerSiteId,
                billingRateId,
                request
        );

        mapper.apply(entity, request);
        repository.save(entity);
    }

    @SuppressWarnings("null")
    public void delete(
            Long customerSiteId,
            Long billingRateId
    ) {
        CustomerSiteBillingRate entity =
                findOwnedRate(customerSiteId, billingRateId);

        repository.delete(entity);
    }

    private CustomerSiteBillingRate findOwnedRate(
            Long customerSiteId,
            Long billingRateId
    ) {
        CustomerSiteBillingRate entity =
                repository.findByIdAndDeletedAtIsNull(billingRateId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "現場請求単価が見つかりません。id="
                                        + billingRateId
                        ));

        if (!customerSiteId.equals(entity.getCustomerSite().getId())) {
            throw new IllegalArgumentException(
                    "現場請求単価の現場IDが一致しません。"
            );
        }

        return entity;
    }

    @SuppressWarnings("null")
    private CustomerSite findCustomerSite(Long customerSiteId) {
        return customerSiteRepository.findById(customerSiteId)
                .filter(site -> site.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException(
                        "顧客現場が見つかりません。id="
                                + customerSiteId
                ));
    }

    private void validate(CustomerSiteBillingRateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "CustomerSiteBillingRateRequest は必須です。"
            );
        }

        if (request.jobCode() == null || request.jobCode().isBlank()) {
            throw new IllegalArgumentException("職種コードは必須です。");
        }

        if (request.jobName() == null || request.jobName().isBlank()) {
            throw new IllegalArgumentException("職種名は必須です。");
        }

        if (request.effectiveFrom() == null) {
            throw new IllegalArgumentException("適用開始日は必須です。");
        }

        if (request.effectiveTo() != null
                && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new IllegalArgumentException(
                    "適用終了日は適用開始日以降にしてください。"
            );
        }

        if (request.baseUnitPrice() != null
                && request.baseUnitPrice().signum() < 0) {
            throw new IllegalArgumentException(
                    "基準単価は0以上で入力してください。"
            );
        }
    }

    private void validateDuplicate(
            Long customerSiteId,
            Long billingRateId,
            CustomerSiteBillingRateRequest request
    ) {
        String siteRoleCode =
                request.siteRoleCode() == null
                        || request.siteRoleCode().isBlank()
                        ? "GENERAL"
                        : request.siteRoleCode().trim();

        boolean exists;

        if (billingRateId == null) {
            exists =
                    repository
                            .existsByCustomerSiteIdAndJobCodeAndSiteRoleCodeAndEffectiveFromAndDeletedAtIsNull(
                                    customerSiteId,
                                    request.jobCode().trim(),
                                    siteRoleCode,
                                    request.effectiveFrom()
                            );
        } else {
            exists =
                    repository
                            .existsByCustomerSiteIdAndJobCodeAndSiteRoleCodeAndEffectiveFromAndIdNotAndDeletedAtIsNull(
                                    customerSiteId,
                                    request.jobCode().trim(),
                                    siteRoleCode,
                                    request.effectiveFrom(),
                                    billingRateId
                            );
        }

        if (exists) {
            throw new IllegalArgumentException(
                    "同じ現場・職種・役職・適用開始日の"
                            + "請求単価が既に登録されています。"
            );
        }
    }
}