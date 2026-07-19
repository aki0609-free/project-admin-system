package com.project.backend.features.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.customer.entity.CustomerTransaction;

public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, Long> {

    List<CustomerTransaction> findByCustomerIdOrderByTargetMonthDesc(Long customerId);

    List<CustomerTransaction> findByCustomerIdOrderByIdAsc(Long customerId);

    Optional<CustomerTransaction> findByCustomerIdAndTargetMonth(
            Long customerId,
            String targetMonth
    );

    void deleteByCustomerId(Long customerId);
}