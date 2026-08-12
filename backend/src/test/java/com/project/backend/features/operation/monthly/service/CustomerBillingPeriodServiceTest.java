package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.operation.monthly.dto.CustomerBillingPeriod;

class CustomerBillingPeriodServiceTest {

    private CustomerBillingPeriodService service;

    @BeforeEach
    void setUp() {
        service = new CustomerBillingPeriodService();
    }

    @Test
    void 月末締めは対象月初日から末日まで() {
        Customer customer = customer(DayRuleType.END_OF_MONTH, null, 0);

        CustomerBillingPeriod result = service.resolve("2026-07", customer);

        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void 二十日締めは前月二十一日から当月二十日まで() {
        Customer customer = customer(DayRuleType.DAY_OF_MONTH, 20, 0);

        CustomerBillingPeriod result = service.resolve("2026-07", customer);

        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 6, 21));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2026, 7, 20));
    }

    @Test
    void 月末に存在しない締日は実在する末日へ補正する() {
        Customer customer = customer(DayRuleType.DAY_OF_MONTH, 31, 0);

        CustomerBillingPeriod result = service.resolve("2027-02", customer);

        assertThat(result.startDate()).isEqualTo(LocalDate.of(2027, 2, 1));
        assertThat(result.endDate()).isEqualTo(LocalDate.of(2027, 2, 28));
    }

    @Test
    void 締日未設定の顧客は処理しない() {
        Customer customer = customer(null, null, 0);

        assertThatThrownBy(() -> service.resolve("2026-07", customer))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("締日区分が未設定");
    }

    private Customer customer(
            DayRuleType type,
            Integer value,
            Integer monthOffset
    ) {
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setClosingDayType(type);
        customer.setClosingDayValue(value);
        customer.setClosingMonthOffset(monthOffset);
        return customer;
    }
}
