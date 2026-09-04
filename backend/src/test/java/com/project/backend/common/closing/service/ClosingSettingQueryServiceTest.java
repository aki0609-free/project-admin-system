package com.project.backend.common.closing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.common.closing.repository.ClosingSettingRepository;
import com.project.backend.common.dayrule.enums.DayRuleType;

class ClosingSettingQueryServiceTest {

    @Test
    void findPayrollSetting_shouldUseOperationalDefaultWhenNotConfigured() {
        ClosingSettingRepository repository = mock(ClosingSettingRepository.class);
        when(repository
                .findFirstBySettingCodeAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(
                        "PAYROLL"
                ))
                .thenReturn(Optional.empty());

        ClosingSettingQueryService service = new ClosingSettingQueryService(repository);

        var setting = service.findPayrollSetting();

        assertThat(setting.getSettingCode()).isEqualTo("PAYROLL");
        assertThat(setting.getClosingDayType()).isEqualTo(DayRuleType.END_OF_MONTH);
        assertThat(setting.getClosingMonthOffset()).isZero();
        assertThat(setting.getPaymentDayType()).isEqualTo(DayRuleType.DAY_OF_MONTH);
        assertThat(setting.getPaymentDayValue()).isEqualTo(25);
        assertThat(setting.getPaymentMonthOffset()).isEqualTo(1);
        assertThat(setting.isActiveFlag()).isTrue();
    }
}
