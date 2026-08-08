package com.project.backend.features.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.customer.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByIdAndDeletedAtIsNull(Long id);

    List<Customer> findByDeletedAtIsNullOrderByIdAsc();

    List<Customer> findByIdInAndDeletedAtIsNullOrderByIdAsc(
            List<Long> ids
    );
}
