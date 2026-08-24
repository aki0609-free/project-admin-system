package com.project.backend.features.master.payrollitem.parameter;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.payrollitem.balance.PayrollItemParameterDefinitionRepository;
import com.project.backend.features.master.payrollitem.balance.PayrollItemParameterOption;

import lombok.RequiredArgsConstructor;

/**
 * 従業員が選択したパラメーター選択肢の計算値をRuleへ渡す汎用Resolver。
 * Resolverキーは SELECT_OPTION_CALCULATION_VALUE:{sourceParameterKey} とする。
 */
@Component
@RequiredArgsConstructor
public class SelectOptionCalculationValueResolver
        implements PayrollItemRuleParameterValueResolver {

    public static final String KEY = "SELECT_OPTION_CALCULATION_VALUE";

    private final PayrollItemParameterDefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public Object resolve(PayrollItemRuleParameterResolutionContext context) {
        String sourceKey = context.resolverArgument();
        if (sourceKey == null || sourceKey.isBlank()) {
            throw new IllegalStateException(
                    "選択肢計算値Resolverの参照パラメーターが未設定です。"
            );
        }
        if (context.balancePolicyId() == null) {
            throw new IllegalStateException(
                    "選択肢計算値Resolverの給与項目ポリシーが未設定です。"
            );
        }
        String selectedValue = context.employeeSettings().get(sourceKey);
        if (selectedValue == null || selectedValue.isBlank()) {
            return BigDecimal.ZERO;
        }

        var definition = definitionRepository
                .findByTenantIdAndBalancePolicyIdAndParameterKeyAndActiveFlagTrueAndDeletedAtIsNull(
                        TenantContext.getTenantId(), context.balancePolicyId(), sourceKey)
                .orElseThrow(() -> new IllegalStateException(
                        "選択肢計算値の参照元が見つかりません。key=" + sourceKey
                ));
        if (definition.getOptionsJson() == null
                || definition.getOptionsJson().isBlank()) {
            throw new IllegalStateException(
                    "選択肢計算値が定義されていません。key=" + sourceKey
            );
        }
        try {
            List<PayrollItemParameterOption> options = objectMapper.readValue(
                    definition.getOptionsJson(),
                    new TypeReference<List<PayrollItemParameterOption>>() { }
            );
            PayrollItemParameterOption option = options.stream()
                    .filter(item -> selectedValue.equals(item.value()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "選択値に対応する計算値がありません。key="
                                    + sourceKey + ", value=" + selectedValue
                    ));
            if (option.calculationValue() == null) {
                throw new IllegalStateException(
                        "選択肢の計算値が未設定です。key="
                                + sourceKey + ", value=" + selectedValue
                );
            }
            return option.calculationValue();
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException(
                    "選択肢計算値を読み込めません。key=" + sourceKey,
                    exception
            );
        }
    }
}
