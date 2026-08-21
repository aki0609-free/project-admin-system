package com.project.backend.features.admin.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.admin.business.entity.DormitoryFeeSetting;
import com.project.backend.features.admin.business.repository.DormitoryFeeSettingRepository;
import com.project.backend.features.employee.enums.DormitoryType;
import com.project.backend.features.master.payrollitem.parameter.PayrollItemRuleParameterResolutionContext;

class DormitoryDailyAmountRuleParameterResolverTest {

    @Test
    void resolve_shouldUseEnrollmentParameterInsteadOfEmployeeColumns() {
        DormitoryFeeSettingRepository repository =
                mock(DormitoryFeeSettingRepository.class);
        DormitoryFeeSetting setting = new DormitoryFeeSetting();
        setting.setDormitoryType(DormitoryType.SHARED_ROOM);
        setting.setDailyAmount(new BigDecimal("450"));
        when(repository.findByDormitoryTypeAndActiveFlagTrueAndDeletedAtIsNull(
                DormitoryType.SHARED_ROOM
        )).thenReturn(Optional.of(setting));

        var resolver = new DormitoryDailyAmountRuleParameterResolver(repository);
        Object result = resolver.resolve(
                new PayrollItemRuleParameterResolutionContext(
                        10L,
                        LocalDate.of(2026, 8, 22),
                        Map.of("dormitoryType", "SHARED_ROOM")
                )
        );

        assertThat(result).isEqualTo(new BigDecimal("450"));
    }

    @Test
    void resolve_shouldReturnZeroWhenDormitoryTypeIsNotConfigured() {
        var resolver = new DormitoryDailyAmountRuleParameterResolver(
                mock(DormitoryFeeSettingRepository.class));

        assertThat(resolver.resolve(
                new PayrollItemRuleParameterResolutionContext(
                        10L, LocalDate.of(2026, 8, 22), Map.of())
        )).isEqualTo(BigDecimal.ZERO);
    }
}
