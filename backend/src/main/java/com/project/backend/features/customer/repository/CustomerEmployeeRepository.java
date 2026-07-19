package com.project.backend.features.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.customer.entity.CustomerEmployee;

import java.util.List;

public interface CustomerEmployeeRepository extends JpaRepository<CustomerEmployee, Long> {
    List<CustomerEmployee> findByCustomerIdOrderByIdAsc(Long customerId);
    void deleteByCustomerId(Long customerId);
}