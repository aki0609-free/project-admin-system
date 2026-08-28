package com.project.backend.features.operation.monthly.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyClosingOutputDefinitionService {

    private static final Set<String> CUSTOMER_BILLING_OUTPUT_CODES = Set.of(
            "MONTHLY_INVOICE",
            "MONTHLY_ORDER_FORM"
    );

    private final MonthlyClosingOutputDefinitionRepository repository;

    public List<MonthlyClosingOutputDefinition> findActive() {
        return repository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc();
    }

    /**
     * 顧客別締日で確定する請求書・注文書を除いた、自社月次締めの実行計画。
     */
    public List<MonthlyClosingOutputDefinition> findActiveCompanyOutputs() {
        return findActive().stream()
                .filter(definition -> !CUSTOMER_BILLING_OUTPUT_CODES.contains(
                        definition.getOutputCode()
                ))
                .toList();
    }
}
