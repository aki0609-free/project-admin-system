package com.project.backend.features.master.payrollitem.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.master.payrollitem.dto.PayrollItemMasterSnapshot;
import com.project.backend.features.master.payrollitem.enums.PayrollItemQueryType;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.provider.PayrollItemValueProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollItemQueryService {

    private final List<PayrollItemValueProvider> providers;

    public List<PayrollItemMasterSnapshot> findDailyItems() {
        return findItems(PayrollItemQueryType.DAILY);
    }

    public List<PayrollItemMasterSnapshot> findDailyItems(
            PayrollItemTargetType targetType
    ) {
        return findItems(
                PayrollItemQueryType.DAILY,
                targetType
        );
    }

    public List<PayrollItemMasterSnapshot> findMonthlyItems() {
        return findItems(PayrollItemQueryType.MONTHLY);
    }

    public List<PayrollItemMasterSnapshot> findMonthlyItems(
            PayrollItemTargetType targetType
    ) {
        return findItems(
                PayrollItemQueryType.MONTHLY,
                targetType
        );
    }

    public List<PayrollItemMasterSnapshot> findPayrollItems() {
        return findItems(PayrollItemQueryType.PAYROLL);
    }

    public List<PayrollItemMasterSnapshot> findBonusItems() {
        return findItems(PayrollItemQueryType.BONUS);
    }

    public List<PayrollItemMasterSnapshot> findItems(
            PayrollItemQueryType queryType
    ) {
        return providers.stream()
                .flatMap(provider -> provider.findItems(queryType).stream())
                .sorted(byDisplayOrder())
                .toList();
    }

    public List<PayrollItemMasterSnapshot> findItems(
            PayrollItemQueryType queryType,
            PayrollItemTargetType targetType
    ) {
        return providers.stream()
                .filter(provider -> provider.supports() == targetType)
                .flatMap(provider -> provider.findItems(queryType).stream())
                .sorted(byDisplayOrder())
                .toList();
    }

    @SuppressWarnings("null")
    private Comparator<PayrollItemMasterSnapshot> byDisplayOrder() {
        return Comparator.comparing(
                PayrollItemMasterSnapshot::displayOrder,
                Comparator.nullsLast(Integer::compareTo)
        );
    }
}