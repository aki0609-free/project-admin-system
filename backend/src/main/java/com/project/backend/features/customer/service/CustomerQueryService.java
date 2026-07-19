package com.project.backend.features.customer.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.dto.CustomerDetailResponse;
import com.project.backend.features.customer.dto.CustomerListItemResponse;
import com.project.backend.features.customer.dto.CustomerOptionItemResponse;
import com.project.backend.features.customer.dto.CustomerOptionResponse;
import com.project.backend.features.customer.dto.CustomerSiteOptionItemResponse;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerTransaction;
import com.project.backend.features.customer.enums.CustomerPaymentStatus;
import com.project.backend.features.customer.mapper.CustomerMapper;
import com.project.backend.features.customer.repository.CustomerEmployeeRepository;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;
import com.project.backend.features.customer.repository.CustomerTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerQueryService {

    private final CustomerRepository customerRepository;
    private final CustomerSiteRepository customerSiteRepository;
    private final CustomerEmployeeRepository customerEmployeeRepository;
    private final CustomerTransactionRepository customerTransactionRepository;
    private final CustomerMapper customerMapper;

    public List<CustomerListItemResponse> findAll() {
        return customerRepository.findAll().stream()
                .map(customer -> customerMapper.toListItem(
                        customer,
                        customerSiteRepository.findByCustomerIdOrderByIdAsc(customer.getId()).size(),
                        customerEmployeeRepository.findByCustomerIdOrderByIdAsc(customer.getId()).size(),
                        getLatestPaymentStatus(customer.getId())
                ))
                .toList();
    }

    @SuppressWarnings("null")
    public CustomerDetailResponse findDetail(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("顧客が見つかりません。id=" + id));

        return customerMapper.toDetail(
                customer,
                customerSiteRepository.findByCustomerIdOrderByIdAsc(id),
                customerEmployeeRepository.findByCustomerIdOrderByIdAsc(id),
                getLatestPaymentStatus(id)
        );
    }

    public CustomerOptionResponse findOptions() {
        List<CustomerOptionItemResponse> customers = customerRepository.findAll()
                .stream()
                .map(customer -> new CustomerOptionItemResponse(
                        customer.getId(),
                        customer.getName()
                ))
                .toList();

        List<CustomerSiteOptionItemResponse> sites = customerSiteRepository.findAll()
                .stream()
                .map(site -> new CustomerSiteOptionItemResponse(
                        site.getId(),
                        site.getCustomerId(),
                        site.getName(),
                        site.getDistanceFromCompanyKm()
                ))
                .toList();

        return new CustomerOptionResponse(customers, sites);
    }

    private String getLatestPaymentStatus(Long customerId) {
        CustomerTransaction latest = customerTransactionRepository
                .findByCustomerIdOrderByTargetMonthDesc(customerId)
                .stream()
                .filter(transaction -> transaction.getTargetMonth() != null)
                .findFirst()
                .orElse(null);

        if (latest == null) {
            return "未";
        }

        return latest.getPaymentStatus() == CustomerPaymentStatus.PAID
                ? "済"
                : "未";
    }
}