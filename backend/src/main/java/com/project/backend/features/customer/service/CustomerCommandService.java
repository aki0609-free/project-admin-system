package com.project.backend.features.customer.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.dto.CustomerEmployeeRequest;
import com.project.backend.features.customer.dto.CustomerSaveRequest;
import com.project.backend.features.customer.dto.CustomerSiteRequest;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerEmployee;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.mapper.CustomerMapper;
import com.project.backend.features.customer.repository.CustomerEmployeeRepository;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerSiteBillingRateRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerCommandService {

    private final CustomerRepository customerRepository;
    private final CustomerSiteRepository customerSiteRepository;
    private final CustomerEmployeeRepository customerEmployeeRepository;
    private final CustomerSiteBillingRateRepository billingRateRepository;
    private final CustomerMapper customerMapper;
    private final CustomerReferenceGuard referenceGuard;
    private final Clock clock;

    public Long create(CustomerSaveRequest request) {
        validate(request);

        @SuppressWarnings("null")
        Customer saved = customerRepository.save(
                customerMapper.toEntity(request)
        );

        syncSites(saved.getId(), request.sites());
        syncEmployees(saved.getId(), request.employees());

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
    }

    private void validate(CustomerSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CustomerSaveRequest は必須です。");
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("顧客名は必須です。");
        }

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
        requests.stream()
                .filter(request -> !Boolean.TRUE.equals(request.isDeleted()))
                .filter(request -> request.name() == null || request.name().isBlank())
                .findFirst()
                .ifPresent(request -> {
                    throw new IllegalArgumentException("現場名は必須です。");
                });
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
        }
    }
}
