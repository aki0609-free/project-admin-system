package com.project.backend.features.customer.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.customer.dto.CustomerSaveRequest;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.enums.CustomerInvoiceType;

class CustomerMapperTest {

    private final CustomerMapper mapper = new CustomerMapper();

    @Test
    void apply_shouldPersistInvoiceTypeAndMonthOffsets() {
        Customer entity = new Customer();

        mapper.apply(entity, request(
                CustomerInvoiceType.PATTERN_3,
                dayRule(0),
                dayRule(1)
        ));

        assertThat(entity.getInvoiceType()).isEqualTo(CustomerInvoiceType.PATTERN_3);
        assertThat(entity.getClosingMonthOffset()).isZero();
        assertThat(entity.getPaymentMonthOffset()).isEqualTo(1);
    }

    @Test
    void toDetail_shouldReturnStoredMonthOffsets() {
        Customer entity = mapper.toEntity(request(
                CustomerInvoiceType.PATTERN_2,
                dayRule(0),
                dayRule(2)
        ));
        entity.setId(10L);

        var response = mapper.toDetail(entity, null, null, "未");

        assertThat(response.invoiceType()).isEqualTo(CustomerInvoiceType.PATTERN_2);
        assertThat(response.closingDayRule().monthOffset()).isZero();
        assertThat(response.paymentDayRule().monthOffset()).isEqualTo(2);
    }

    private CustomerSaveRequest request(
            CustomerInvoiceType invoiceType,
            DayRule closingRule,
            DayRule paymentRule
    ) {
        return new CustomerSaveRequest(
                "テスト顧客", null, null, null, null, null, null,
                null, null, invoiceType, closingRule, paymentRule, null, null
        );
    }

    private DayRule dayRule(int monthOffset) {
        return new DayRule(DayRuleType.END_OF_MONTH, null, monthOffset);
    }
}
