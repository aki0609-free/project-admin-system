package com.project.backend.features.operation.monthly.service;

import java.util.List;
import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.enums.CustomerContractStatus;
import com.project.backend.features.operation.monthly.dto.CustomerBillingPeriod;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerBillingTargetService {

    private final CustomerRepository customerRepository;
    private final CustomerBillingPeriodService periodService;
    private final MonthlyInvoiceTargetCustomerQueryService targetQueryService;

    public List<Target> findTargets(String targetMonth) {
        YearMonth month = YearMonth.parse(targetMonth);
        List<Long> candidateIds = targetQueryService.findTargetCustomerIds(
                month.minusMonths(2).atDay(1),
                month.plusMonths(2).atEndOfMonth()
        );
        return customerRepository.findByDeletedAtIsNullOrderByIdAsc()
                .stream()
                .filter(customer -> customer.getContractFlag() == CustomerContractStatus.ACTIVE)
                .filter(customer -> candidateIds.contains(customer.getId()))
                .map(customer -> new Target(
                        customer,
                        periodService.resolve(targetMonth, customer)
                ))
                .filter(target -> targetQueryService.hasTargetData(
                        target.customer().getId(),
                        target.period().startDate(),
                        target.period().endDate()
                ))
                .toList();
    }

    public Target findTarget(String targetMonth, Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("customerIdは必須です。");
        }
        return findTargets(targetMonth).stream()
                .filter(target -> customerId.equals(target.customer().getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "指定した顧客に請求対象データがありません。customerId="
                                + customerId
                ));
    }

    public record Target(
            Customer customer,
            CustomerBillingPeriod period
    ) {
    }
}
