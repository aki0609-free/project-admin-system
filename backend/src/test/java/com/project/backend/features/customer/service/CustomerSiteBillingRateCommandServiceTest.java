package com.project.backend.features.customer.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.dto.CustomerSiteBillingRateRequest;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.enums.CustomerBillingUnit;
import com.project.backend.features.customer.mapper.CustomerSiteBillingRateMapper;
import com.project.backend.features.customer.repository.CustomerSiteBillingRateRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;

class CustomerSiteBillingRateCommandServiceTest {

    private final CustomerSiteRepository siteRepository =
            mock(CustomerSiteRepository.class);
    private final CustomerSiteBillingRateRepository rateRepository =
            mock(CustomerSiteBillingRateRepository.class);
    private final CustomerSiteBillingRateCommandService service =
            new CustomerSiteBillingRateCommandService(
                    siteRepository,
                    rateRepository,
                    new CustomerSiteBillingRateMapper()
            );

    @Test
    void create_shouldUseSiteIdFromRequestAndValidateCustomerOwnership() {
        CustomerSite foreignSite = site(20L, 99L);
        when(siteRepository.findByIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(foreignSite));

        assertThatThrownBy(() -> service.create(10L, request(20L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("顧客IDが一致しません");

        verify(rateRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectOverlappingEffectivePeriod() {
        CustomerSite site = site(20L, 10L);
        when(siteRepository.findByIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(site));
        when(rateRepository.existsOverlappingRate(
                20L,
                "WORKER",
                "GENERAL",
                LocalDate.of(2026, 8, 1),
                null,
                null
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(10L, request(20L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("適用期間が重複");

        verify(rateRepository, never()).save(any());
    }

    @Test
    void create_shouldRejectNegativeNonBaseUnitPrice() {
        CustomerSiteBillingRateRequest request = new CustomerSiteBillingRateRequest(
                null,
                20L,
                "WORKER",
                "作業員",
                "GENERAL",
                "一般",
                CustomerBillingUnit.DAILY,
                BigDecimal.valueOf(10_000),
                BigDecimal.valueOf(-1),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.of(2026, 8, 1),
                null,
                1,
                true,
                null,
                true,
                false,
                false
        );

        assertThatThrownBy(() -> service.create(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("残業単価は0以上で入力してください。");

        verify(siteRepository, never()).findByIdAndDeletedAtIsNull(any());
        verify(rateRepository, never()).save(any());
    }

    private CustomerSite site(Long siteId, Long customerId) {
        CustomerSite site = new CustomerSite();
        site.setId(siteId);
        site.setCustomerId(customerId);
        site.setName("テスト現場");
        return site;
    }

    private CustomerSiteBillingRateRequest request(
            Long customerSiteId,
            Long id
    ) {
        return new CustomerSiteBillingRateRequest(
                id,
                customerSiteId,
                "WORKER",
                "作業員",
                "GENERAL",
                "一般",
                CustomerBillingUnit.DAILY,
                BigDecimal.valueOf(10_000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.of(2026, 8, 1),
                null,
                1,
                true,
                null,
                id == null,
                id != null,
                false
        );
    }
}
