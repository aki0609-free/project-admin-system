package com.project.backend.features.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.customer.entity.CustomerTransaction;

public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, Long> {

    boolean existsByCustomerIdAndDeletedAtIsNull(Long customerId);

    List<CustomerTransaction> findByDeletedAtIsNullOrderByTargetMonthDescIdDesc();

    List<CustomerTransaction> findByCustomerIdAndDeletedAtIsNullOrderByTargetMonthDesc(Long customerId);

    List<CustomerTransaction> findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(Long customerId);

    Optional<CustomerTransaction> findByIdAndDeletedAtIsNull(Long id);

    Optional<CustomerTransaction> findByCustomerIdAndTargetMonthAndDeletedAtIsNull(
            Long customerId,
            String targetMonth
    );

    boolean existsByCustomerIdAndTargetMonthAndDeletedAtIsNull(
            Long customerId,
            String targetMonth
    );

    boolean existsByCustomerIdAndTargetMonthAndIdNotAndDeletedAtIsNull(
            Long customerId,
            String targetMonth,
            Long id
    );
}
