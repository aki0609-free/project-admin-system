package com.project.backend.features.customer.service;

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
import com.project.backend.features.customer.repository.CustomerSiteRepository;
import com.project.backend.features.customer.repository.CustomerTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerCommandService {

    private final CustomerRepository customerRepository;
    private final CustomerSiteRepository customerSiteRepository;
    private final CustomerEmployeeRepository customerEmployeeRepository;
    private final CustomerTransactionRepository customerTransactionRepository;
    private final CustomerMapper customerMapper;

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
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("顧客が見つかりません。id=" + id));

        customerMapper.apply(customer, request);
        customerRepository.save(customer);

        syncSites(id, request.sites());
        syncEmployees(id, request.employees());
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        @SuppressWarnings("null")
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("顧客が見つかりません。id=" + id));

        customerTransactionRepository.deleteByCustomerId(id);
        customerSiteRepository.deleteByCustomerId(id);
        customerEmployeeRepository.deleteByCustomerId(id);
        customerRepository.delete(customer);
    }

    private void validate(CustomerSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CustomerSaveRequest は必須です。");
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("顧客名は必須です。");
        }
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
                if (request.id() != null && customerSiteRepository.existsById(request.id())) {
                    customerSiteRepository.deleteById(request.id());
                }
                continue;
            }

            if (Boolean.TRUE.equals(request.isNew())
                    || request.id() == null
                    || !customerSiteRepository.existsById(request.id())) {
                customerSiteRepository.save(
                        customerMapper.toSiteEntity(customerId, request)
                );
                continue;
            }

            @SuppressWarnings("null")
            CustomerSite entity = customerSiteRepository.findById(request.id())
                    .orElseThrow(() -> new IllegalArgumentException("現場が見つかりません。id=" + request.id()));

            validateCustomerOwnership(entity.getCustomerId(), customerId, "現場");

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
                if (request.id() != null && customerEmployeeRepository.existsById(request.id())) {
                    customerEmployeeRepository.deleteById(request.id());
                }
                continue;
            }

            if (Boolean.TRUE.equals(request.isNew())
                    || request.id() == null
                    || !customerEmployeeRepository.existsById(request.id())) {
                customerEmployeeRepository.save(
                        customerMapper.toEmployeeEntity(customerId, request)
                );
                continue;
            }

            @SuppressWarnings("null")
            CustomerEmployee entity = customerEmployeeRepository.findById(request.id())
                    .orElseThrow(() -> new IllegalArgumentException("顧客社員が見つかりません。id=" + request.id()));

            validateCustomerOwnership(entity.getCustomerId(), customerId, "顧客社員");

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
}