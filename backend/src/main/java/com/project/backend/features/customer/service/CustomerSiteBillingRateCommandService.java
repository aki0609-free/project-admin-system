package com.project.backend.features.customer.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.dto.CustomerSiteBillingRateBulkSaveRequest;
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

    private static final BigDecimal MAX_UNIT_PRICE =
            new BigDecimal("9999999999999.99");

    private final CustomerSiteRepository customerSiteRepository;
    private final CustomerSiteBillingRateRepository repository;
    private final CustomerSiteBillingRateMapper mapper;

    /**
     * 画面上の一括保存を1トランザクションで確定する。
     * 途中の1件で失敗した場合は、削除・追加・更新をすべてロールバックする。
     */
    public void bulkSave(
            Long customerId,
            CustomerSiteBillingRateBulkSaveRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "CustomerSiteBillingRateBulkSaveRequest は必須です。"
            );
        }

        List<Long> deletedIds = safeList(request.deletedIds());
        List<CustomerSiteBillingRateRequest> created =
                safeList(request.created());
        List<CustomerSiteBillingRateRequest> updated =
                safeList(request.updated());

        for (Long billingRateId : deletedIds) {
            if (billingRateId == null || billingRateId <= 0) {
                throw new IllegalArgumentException(
                        "削除対象の請求単価IDが不正です。"
                );
            }
            delete(customerId, billingRateId);
        }

        // 同じ一意キーで削除後に再登録できるよう、DELETEを先にDBへ反映する。
        if (!deletedIds.isEmpty()) {
            repository.flush();
        }

        for (CustomerSiteBillingRateRequest item : created) {
            create(customerId, item);
        }

        for (CustomerSiteBillingRateRequest item : updated) {
            if (item == null || item.id() == null || item.id() <= 0) {
                throw new IllegalArgumentException(
                        "更新対象の請求単価IDが不正です。"
                );
            }
            update(customerId, item.id(), item);
        }
    }

    @SuppressWarnings("null")
    public Long create(
            Long customerId,
            CustomerSiteBillingRateRequest request
    ) {
        validate(request);

        CustomerSite customerSite = findCustomerSite(
                customerId,
                request.customerSiteId()
        );

        validateEffectivePeriodOverlap(
                customerSite.getId(),
                null,
                request
        );

        CustomerSiteBillingRate entity =
                mapper.toEntity(customerSite, request);

        return repository.save(entity).getId();
    }

    @SuppressWarnings("null")
    public void update(
            Long customerId,
            Long billingRateId,
            CustomerSiteBillingRateRequest request
    ) {
        validate(request);

        CustomerSiteBillingRate entity =
                findOwnedRate(customerId, billingRateId);

        CustomerSite customerSite = findCustomerSite(
                customerId,
                request.customerSiteId()
        );

        validateEffectivePeriodOverlap(
                customerSite.getId(),
                billingRateId,
                request
        );

        entity.setCustomerSite(customerSite);
        mapper.apply(entity, request);
        repository.save(entity);
    }

    @SuppressWarnings("null")
    public void delete(
            Long customerId,
            Long billingRateId
    ) {
        CustomerSiteBillingRate entity =
                findOwnedRate(customerId, billingRateId);

        repository.delete(entity);
    }

    private CustomerSiteBillingRate findOwnedRate(
            Long customerId,
            Long billingRateId
    ) {
        CustomerSiteBillingRate entity =
                repository.findByIdAndDeletedAtIsNull(billingRateId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "現場請求単価が見つかりません。id="
                                        + billingRateId
                        ));

        if (!customerId.equals(entity.getCustomerSite().getCustomerId())) {
            throw new IllegalArgumentException(
                    "現場請求単価の顧客IDが一致しません。"
            );
        }

        return entity;
    }

    @SuppressWarnings("null")
    private CustomerSite findCustomerSite(
            Long customerId,
            Long customerSiteId
    ) {
        if (customerSiteId == null) {
            throw new IllegalArgumentException("現場は必須です。");
        }

        CustomerSite site = customerSiteRepository
                .findByIdAndDeletedAtIsNull(customerSiteId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "顧客現場が見つかりません。id="
                                + customerSiteId
                ));

        if (!customerId.equals(site.getCustomerId())) {
            throw new IllegalArgumentException(
                    "顧客現場の顧客IDが一致しません。"
            );
        }

        return site;
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
        validateLength(request.jobCode(), 100, "職種コード");

        if (request.jobName() == null || request.jobName().isBlank()) {
            throw new IllegalArgumentException("職種名は必須です。");
        }
        validateLength(request.jobName(), 200, "職種名");

        validateLength(request.siteRoleCode(), 100, "役職コード");
        validateLength(request.siteRoleName(), 200, "現場役職");
        validateLength(request.note(), 1000, "備考");

        if (request.billingUnit() == null) {
            throw new IllegalArgumentException("請求単位は必須です。");
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

        validateNonNegative(request.baseUnitPrice(), "基準単価");
        validateNonNegative(request.overtimeUnitPrice(), "残業単価");
        validateNonNegative(request.nightUnitPrice(), "深夜単価");
        validateNonNegative(request.holidayUnitPrice(), "休日単価");
        validateNonNegative(request.commuteUnitPrice(), "通勤単価");

        if (request.displayOrder() != null && request.displayOrder() < 1) {
            throw new IllegalArgumentException("表示順は1以上で入力してください。");
        }
    }

    private void validateNonNegative(java.math.BigDecimal value, String label) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(
                    label + "は0以上で入力してください。"
            );
        }
        if (value != null && value.compareTo(MAX_UNIT_PRICE) > 0) {
            throw new IllegalArgumentException(
                    label + "は9,999,999,999,999.99以下で入力してください。"
            );
        }
        if (value != null && value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    label + "は小数第2位までで入力してください。"
            );
        }
    }

    private void validateLength(String value, int maxLength, String label) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(
                    label + "は" + maxLength + "文字以内で入力してください。"
            );
        }
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void validateEffectivePeriodOverlap(
            Long customerSiteId,
            Long billingRateId,
            CustomerSiteBillingRateRequest request
    ) {
        String siteRoleCode =
                request.siteRoleCode() == null
                        || request.siteRoleCode().isBlank()
                        ? "GENERAL"
                        : request.siteRoleCode().trim();

        boolean exists = repository.existsOverlappingRate(
                customerSiteId,
                request.jobCode().trim(),
                siteRoleCode,
                request.effectiveFrom(),
                request.effectiveTo(),
                billingRateId
        );

        if (exists) {
            throw new IllegalArgumentException(
                    "同じ現場・職種・役職で適用期間が重複する"
                            + "請求単価が既に登録されています。"
            );
        }
    }
}
