package com.project.backend.features.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.customer.entity.CustomerEmployee;

import java.util.List;
import java.util.Optional;

public interface CustomerEmployeeRepository extends JpaRepository<CustomerEmployee, Long> {
    Optional<CustomerEmployee> findByIdAndDeletedAtIsNull(Long id);
    List<CustomerEmployee> findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(Long customerId);
}
