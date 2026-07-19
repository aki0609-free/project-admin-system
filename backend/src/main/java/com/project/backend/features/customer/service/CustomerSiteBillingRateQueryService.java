package com.project.backend.features.customer.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.dto.CustomerSiteBillingRateResponse;
import com.project.backend.features.customer.entity.CustomerSiteBillingRate;
import com.project.backend.features.customer.mapper.CustomerSiteBillingRateMapper;
import com.project.backend.features.customer.repository.CustomerSiteBillingRateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerSiteBillingRateQueryService {

    private final CustomerSiteBillingRateRepository repository;
    private final CustomerSiteBillingRateMapper mapper;

    /**
     * 顧客に紐づく全現場の請求単価を取得する。
     *
     * 顧客編集画面の「請求単価」タブで使用する。
     */
    public List<CustomerSiteBillingRateResponse> findByCustomerId(
            Long customerId
    ) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerId は必須です。");
        }

        return repository
                .findByCustomerSiteCustomerIdAndDeletedAtIsNullOrderByCustomerSiteIdAscDisplayOrderAscIdAsc(
                        customerId
                )
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * 現場に紐づく請求単価を取得する。
     *
     * 現場単位で請求単価を扱う機能や、
     * 将来の現場詳細画面などで使用できる。
     */
    public List<CustomerSiteBillingRateResponse> findByCustomerSiteId(
            Long customerSiteId
    ) {
        if (customerSiteId == null) {
            throw new IllegalArgumentException(
                    "customerSiteId は必須です。"
            );
        }

        return repository
                .findByCustomerSiteIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        customerSiteId
                )
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * IDで請求単価を取得する。
     */
    public CustomerSiteBillingRateResponse findById(Long id) {
        return mapper.toResponse(findEntityById(id));
    }

    /**
     * 顧客IDと請求単価IDの両方を検証して取得する。
     *
     * 顧客単位APIで、別顧客の請求単価へアクセスされることを防ぐ。
     */
    public CustomerSiteBillingRateResponse findByCustomerIdAndId(
            Long customerId,
            Long billingRateId
    ) {
        CustomerSiteBillingRate entity =
                findEntityById(billingRateId);

        Long entityCustomerId =
                entity.getCustomerSite().getCustomerId();

        if (!customerId.equals(entityCustomerId)) {
            throw new IllegalArgumentException(
                    "指定された請求単価は対象顧客に属していません。"
                            + " customerId=" + customerId
                            + ", billingRateId=" + billingRateId
            );
        }

        return mapper.toResponse(entity);
    }

    /**
     * 勤務日に適用される請求単価を取得する。
     *
     * 日報保存時に、
     * 現場・職種・役職・勤務日から単価を解決するために使用する。
     */
    public CustomerSiteBillingRate findApplicableRate(
            Long customerSiteId,
            String jobCode,
            String siteRoleCode,
            LocalDate workDate
    ) {
        validateLookup(
                customerSiteId,
                jobCode,
                siteRoleCode,
                workDate
        );

        return repository.findApplicableRates(
                        customerSiteId,
                        jobCode.trim(),
                        siteRoleCode.trim(),
                        workDate
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "適用可能な請求単価が見つかりません。"
                                + " customerSiteId=" + customerSiteId
                                + ", jobCode=" + jobCode
                                + ", siteRoleCode=" + siteRoleCode
                                + ", workDate=" + workDate
                ));
    }

    /**
     * Entityを内部処理で取得する。
     */
    private CustomerSiteBillingRate findEntityById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "billingRateId は必須です。"
            );
        }

        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "現場請求単価が見つかりません。id=" + id
                ));
    }

    private void validateLookup(
            Long customerSiteId,
            String jobCode,
            String siteRoleCode,
            LocalDate workDate
    ) {
        if (customerSiteId == null) {
            throw new IllegalArgumentException(
                    "customerSiteId は必須です。"
            );
        }

        if (jobCode == null || jobCode.isBlank()) {
            throw new IllegalArgumentException(
                    "jobCode は必須です。"
            );
        }

        if (siteRoleCode == null || siteRoleCode.isBlank()) {
            throw new IllegalArgumentException(
                    "siteRoleCode は必須です。"
            );
        }

        if (workDate == null) {
            throw new IllegalArgumentException(
                    "workDate は必須です。"
            );
        }
    }
}