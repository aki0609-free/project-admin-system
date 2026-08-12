package com.project.backend.features.operation.monthly.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.CustomerBillingClosing;

public interface CustomerBillingClosingRepository
        extends JpaRepository<CustomerBillingClosing, Long> {

    Optional<CustomerBillingClosing>
            findByTargetMonthAndCustomerIdAndDeletedAtIsNull(
                    LocalDate targetMonth,
                    Long customerId
            );

    List<CustomerBillingClosing>
            findAllByTargetMonthAndCustomerIdIsNotNullAndDeletedAtIsNull(
                    LocalDate targetMonth
            );

    List<CustomerBillingClosing>
            findAllByTargetMonthAndCustomerIdInAndDeletedAtIsNull(
                    LocalDate targetMonth,
                    List<Long> customerIds
            );
}
