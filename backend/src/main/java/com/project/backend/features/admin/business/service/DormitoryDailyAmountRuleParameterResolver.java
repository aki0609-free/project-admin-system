package com.project.backend.features.admin.business.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.project.backend.features.admin.business.repository.DormitoryFeeSettingRepository;
import com.project.backend.features.employee.enums.DormitoryType;
import com.project.backend.features.master.payrollitem.parameter.PayrollItemRuleParameterResolutionContext;
import com.project.backend.features.master.payrollitem.parameter.PayrollItemRuleParameterValueResolver;

import lombok.RequiredArgsConstructor;

/**
 * Fuyo固有の寮タイプ別基準単価を、汎用給与項目Ruleへ供給するアダプター。
 */
@Component
@RequiredArgsConstructor
public class DormitoryDailyAmountRuleParameterResolver
        implements PayrollItemRuleParameterValueResolver {

    public static final String KEY = "DORMITORY_DAILY_AMOUNT";

    private final DormitoryFeeSettingRepository repository;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Object resolve(PayrollItemRuleParameterResolutionContext context) {
        String dormitoryType = context.employeeSettings().get("dormitoryType");
        if (dormitoryType == null || dormitoryType.isBlank()) {
            return BigDecimal.ZERO;
        }
        DormitoryType type;
        try {
            type = DormitoryType.valueOf(dormitoryType);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "寮タイプ設定が不正です。value=" + dormitoryType,
                    exception
            );
        }
        return repository.findByDormitoryTypeAndActiveFlagTrueAndDeletedAtIsNull(type)
                .map(setting -> setting.getDailyAmount())
                .orElse(BigDecimal.ZERO);
    }
}
