package com.project.backend.features.customer.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.dto.CustomerEmployeeRequest;
import com.project.backend.features.customer.dto.CustomerSaveRequest;
import com.project.backend.features.customer.dto.CustomerSiteRequest;
import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerEmployee;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.mapper.CustomerMapper;
import com.project.backend.features.customer.repository.CustomerEmployeeRepository;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerSiteBillingRateRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;
import com.project.backend.features.customer.service.integration.CustomerInvoiceMailGroupSynchronizer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerCommandService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    private final CustomerRepository customerRepository;
    private final CustomerSiteRepository customerSiteRepository;
    private final CustomerEmployeeRepository customerEmployeeRepository;
    private final CustomerSiteBillingRateRepository billingRateRepository;
    private final CustomerMapper customerMapper;
    private final CustomerReferenceGuard referenceGuard;
    private final CustomerInvoiceMailGroupSynchronizer invoiceMailGroupSynchronizer;
    private final Clock clock;

    public Long create(CustomerSaveRequest request) {
        validate(request);

        @SuppressWarnings("null")
        Customer saved = customerRepository.save(
                customerMapper.toEntity(request)
        );

        syncSites(saved.getId(), request.sites());
        syncEmployees(saved.getId(), request.employees());
        invoiceMailGroupSynchronizer.synchronize(saved.getId(), saved.getName());

        return saved.getId();
    }

    @SuppressWarnings("null")
    public void update(
            Long id,
            CustomerSaveRequest request
    ) {
        validate(request);

        @SuppressWarnings("null")
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("顧客が見つかりません。id=" + id));

        customerMapper.apply(customer, request);
        customerRepository.save(customer);

        syncSites(id, request.sites());
        syncEmployees(id, request.employees());
        invoiceMailGroupSynchronizer.synchronize(id, customer.getName());
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        @SuppressWarnings("null")
        Customer customer = customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("顧客が見つかりません。id=" + id));

        referenceGuard.assertCustomerDeletable(id);

        Instant deletedAt = Instant.now(clock);

        for (CustomerSite site : customerSiteRepository
                .findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(id)) {
            softDeleteBillingRates(site.getId(), deletedAt);
            site.setDeletedAt(deletedAt);
        }

        for (CustomerEmployee employee : customerEmployeeRepository
                .findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(id)) {
            employee.setDeletedAt(deletedAt);
        }

        customer.setDeletedAt(deletedAt);
        invoiceMailGroupSynchronizer.delete(id);
    }

    private void validate(CustomerSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CustomerSaveRequest は必須です。");
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("顧客名は必須です。");
        }

        validateLength(request.name(), 255, "顧客名");
        validateLength(request.furiganaName(), 255, "ふりがな");
        validateLength(request.shortName(), 255, "短縮社名");
        validateLength(request.postNo(), 255, "郵便番号");
        validateLength(request.address(), 255, "住所");
        validateLength(request.representativeName(), 255, "代表者名");
        validateLength(request.phone(), 255, "電話番号");
        validateLength(request.jobType(), 255, "職種");
        validateDayRule(request.closingDayRule(), "締日");
        validateDayRule(request.paymentDayRule(), "支払日");

        validateSites(request.sites());
        validateEmployees(request.employees());
    }

    @SuppressWarnings("null")
    private void syncSites(
            Long customerId,
            List<CustomerSiteRequest> requests
    ) {
        if (requests == null) {
            return;
        }

        for (CustomerSiteRequest request : requests) {
            if (Boolean.TRUE.equals(request.isDeleted())) {
                if (request.id() != null) {
                    CustomerSite entity = findOwnedSite(customerId, request.id());
                    referenceGuard.assertSiteDeletable(entity.getId());
                    Instant deletedAt = Instant.now(clock);
                    softDeleteBillingRates(entity.getId(), deletedAt);
                    entity.setDeletedAt(deletedAt);
                }
                continue;
            }

            if (Boolean.TRUE.equals(request.isNew())
                    || request.id() == null) {
                customerSiteRepository.save(
                        customerMapper.toSiteEntity(customerId, request)
                );
                continue;
            }

            CustomerSite entity = findOwnedSite(customerId, request.id());

            customerMapper.applySite(entity, request);
            customerSiteRepository.save(entity);
        }
    }

    @SuppressWarnings("null")
    private void syncEmployees(
            Long customerId,
            List<CustomerEmployeeRequest> requests
    ) {
        if (requests == null) {
            return;
        }

        for (CustomerEmployeeRequest request : requests) {
            if (Boolean.TRUE.equals(request.isDeleted())) {
                if (request.id() != null) {
                    CustomerEmployee entity = findOwnedEmployee(customerId, request.id());
                    entity.setDeletedAt(Instant.now(clock));
                }
                continue;
            }

            if (Boolean.TRUE.equals(request.isNew())
                    || request.id() == null) {
                customerEmployeeRepository.save(
                        customerMapper.toEmployeeEntity(customerId, request)
                );
                continue;
            }

            CustomerEmployee entity = findOwnedEmployee(customerId, request.id());

            customerMapper.applyEmployee(entity, request);
            customerEmployeeRepository.save(entity);
        }
    }

    private void validateCustomerOwnership(
            Long entityCustomerId,
            Long requestCustomerId,
            String label
    ) {
        if (!requestCustomerId.equals(entityCustomerId)) {
            throw new IllegalArgumentException(label + "の顧客IDが一致しません。");
        }
    }

    private CustomerSite findOwnedSite(Long customerId, Long siteId) {
        CustomerSite entity = customerSiteRepository.findByIdAndDeletedAtIsNull(siteId)
                .orElseThrow(() -> new IllegalArgumentException("現場が見つかりません。id=" + siteId));
        validateCustomerOwnership(entity.getCustomerId(), customerId, "現場");
        return entity;
    }

    private CustomerEmployee findOwnedEmployee(Long customerId, Long employeeId) {
        CustomerEmployee entity = customerEmployeeRepository.findByIdAndDeletedAtIsNull(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("顧客社員が見つかりません。id=" + employeeId));
        validateCustomerOwnership(entity.getCustomerId(), customerId, "顧客社員");
        return entity;
    }

    private void softDeleteBillingRates(Long customerSiteId, Instant deletedAt) {
        billingRateRepository
                .findByCustomerSiteIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(customerSiteId)
                .forEach(rate -> rate.setDeletedAt(deletedAt));
    }

    private void validateSites(List<CustomerSiteRequest> requests) {
        if (requests == null) {
            return;
        }
        for (CustomerSiteRequest request : requests) {
            if (Boolean.TRUE.equals(request.isDeleted())) {
                continue;
            }
            if (request.name() == null || request.name().isBlank()) {
                throw new IllegalArgumentException("現場名は必須です。");
            }
            validateLength(request.name(), 255, "現場名");
            validateLength(request.contactPersonName(), 255, "現場担当者名");
            validateLength(request.contactPersonPhone(), 255, "現場担当者電話番号");
            validateEmail(request.contactPersonEmail(), "現場担当者メールアドレス");
            if (request.distanceFromCompanyKm() != null
                    && request.distanceFromCompanyKm() < 0) {
                throw new IllegalArgumentException("会社からの距離は0以上で入力してください。");
            }
        }
    }

    private void validateEmployees(List<CustomerEmployeeRequest> requests) {
        if (requests == null) {
            return;
        }
        for (CustomerEmployeeRequest request : requests) {
            if (Boolean.TRUE.equals(request.isDeleted())) {
                continue;
            }
            if (request.name() == null || request.name().isBlank()) {
                throw new IllegalArgumentException("顧客担当者名は必須です。");
            }
            if ((Boolean.TRUE.equals(request.invoiceToFlag())
                    || Boolean.TRUE.equals(request.invoiceCcFlag()))
                    && (request.email() == null || request.email().isBlank())) {
                throw new IllegalArgumentException(
                        "請求書のToまたはCCに指定する担当者はメールアドレスが必須です。"
                );
            }
            if (Boolean.TRUE.equals(request.invoiceToFlag())
                    && Boolean.TRUE.equals(request.invoiceCcFlag())) {
                throw new IllegalArgumentException(
                        "同じ顧客担当者を請求書のToとCCへ同時に指定することはできません。"
                );
            }
            validateLength(request.name(), 255, "顧客担当者名");
            validateLength(request.furiganaName(), 255, "顧客担当者ふりがな");
            validateLength(request.position(), 255, "顧客担当者役職");
            validateLength(request.phone(), 255, "顧客担当者電話番号");
            validateEmail(request.email(), "顧客担当者メールアドレス");
        }
    }

    private void validateDayRule(DayRule rule, String label) {
        if (rule == null) {
            return;
        }
        if (rule.type() == null) {
            throw new IllegalArgumentException(label + "の種別は必須です。");
        }
        int monthOffset = rule.monthOffset() == null ? 0 : rule.monthOffset();
        if (monthOffset < 0 || monthOffset > 12) {
            throw new IllegalArgumentException(label + "の月オフセットは0から12で入力してください。");
        }
        if (rule.type() == DayRuleType.DAY_OF_MONTH
                && (rule.value() == null || rule.value() < 1 || rule.value() > 31)) {
            throw new IllegalArgumentException(label + "は1日から31日の範囲で入力してください。");
        }
    }

    private void validateEmail(String value, String label) {
        if (value == null || value.isBlank()) {
            return;
        }
        validateLength(value, 255, label);
        if (!EMAIL_PATTERN.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(label + "の形式が正しくありません。");
        }
    }

    private void validateLength(String value, int maxLength, String label) {
        if (value != null && value.trim().length() > maxLength) {
            throw new IllegalArgumentException(label + "は" + maxLength + "文字以内で入力してください。");
        }
    }
}
