package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;

class MonthlyClosingOutputDefinitionServiceTest {

    @Test
    void findActiveCompanyOutputs_shouldExcludeCustomerClosingDocuments() {
        MonthlyClosingOutputDefinition paySlip = definition(
                "MONTHLY_PAY_SLIP"
        );
        MonthlyClosingOutputDefinition invoice = definition(
                "MONTHLY_INVOICE"
        );
        MonthlyClosingOutputDefinition orderForm = definition(
                "MONTHLY_ORDER_FORM"
        );
        MonthlyClosingOutputDefinition ledger = definition(
                "MONTHLY_LABOR_LEDGER"
        );
        MonthlyClosingOutputDefinitionRepository repository = mock(
                MonthlyClosingOutputDefinitionRepository.class
        );
        when(repository
                .findByActiveFlagTrueAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc())
                .thenReturn(List.of(paySlip, invoice, orderForm, ledger));
        MonthlyClosingOutputDefinitionService service =
                new MonthlyClosingOutputDefinitionService(repository);

        assertThat(service.findActiveCompanyOutputs())
                .extracting(MonthlyClosingOutputDefinition::getOutputCode)
                .containsExactly(
                        "MONTHLY_PAY_SLIP",
                        "MONTHLY_LABOR_LEDGER"
                );
    }

    private MonthlyClosingOutputDefinition definition(String outputCode) {
        MonthlyClosingOutputDefinition definition =
                new MonthlyClosingOutputDefinition();
        definition.setOutputCode(outputCode);
        return definition;
    }
}
