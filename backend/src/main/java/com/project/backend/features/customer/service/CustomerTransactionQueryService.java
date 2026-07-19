package com.project.backend.features.customer.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.dto.CustomerTransactionResponse;
import com.project.backend.features.customer.mapper.CustomerTransactionMapper;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerTransactionQueryService {

    private final CustomerRepository customerRepository;
    private final CustomerTransactionRepository repository;
    private final CustomerTransactionMapper mapper;

    public List<CustomerTransactionResponse> findAll(Long customerId) {
        if (customerId != null) {
            return findByCustomerId(customerId);
        }

        return repository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<CustomerTransactionResponse> findByCustomerId(Long customerId) {
        validateCustomerExists(customerId);

        return repository.findByCustomerIdOrderByTargetMonthDesc(customerId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @SuppressWarnings("null")
    private void validateCustomerExists(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new IllegalArgumentException("顧客が見つかりません。id=" + customerId);
        }
    }
}