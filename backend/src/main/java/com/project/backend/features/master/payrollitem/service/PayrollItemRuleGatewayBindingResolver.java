package com.project.backend.features.master.payrollitem.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.master.payrollitem.dto.PayrollItemMasterSnapshot;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.provider.PayrollItemValueProvider;
import com.project.backend.features.system.rule.gateway.RuleGatewayBinding;
import com.project.backend.features.system.rule.gateway.RuleGatewayBindingResolver;

@Component
public class PayrollItemRuleGatewayBindingResolver
        implements RuleGatewayBindingResolver {

    private final Map<PayrollItemTargetType, PayrollItemValueProvider>
            providerMap;

    public PayrollItemRuleGatewayBindingResolver(
            List<PayrollItemValueProvider> providers
    ) {
        this.providerMap = providers.stream().collect(Collectors.toMap(
                PayrollItemValueProvider::supports,
                Function.identity()
        ));
    }

    @Override
    public boolean supports(String gatewayKey) {
        return PayrollItemRuleGatewayKeys.supports(gatewayKey);
    }

    @Override
    public RuleGatewayBinding resolve(
            String gatewayKey,
            LocalDate targetDate
    ) {
        PayrollItemRuleGatewayKeys.Descriptor descriptor =
                PayrollItemRuleGatewayKeys.parse(gatewayKey);
        PayrollItemValueProvider provider = providerMap.get(
                descriptor.targetType()
        );
        if (provider == null) {
            throw new IllegalStateException(
                    "給与項目Providerがありません。targetType="
                            + descriptor.targetType()
            );
        }
        PayrollItemMasterSnapshot master = provider.findMaster(
                new PayrollItemValueRequest(
                        descriptor.targetType(),
                        descriptor.masterId(),
                        null,
                        null,
                        Map.of()
                )
        );
        if (!"AUTO".equals(master.calculationType())
                || !StringUtils.hasText(master.ruleName())) {
            throw new IllegalStateException(
                    "AUTO Rule給与項目ではありません。targetType="
                            + descriptor.targetType()
                            + ", masterId="
                            + descriptor.masterId()
            );
        }
        return binding(master);
    }

    public static RuleGatewayBinding binding(
            PayrollItemMasterSnapshot master
    ) {
        String gatewayKey = PayrollItemRuleGatewayKeys.forMaster(
                master.targetType(),
                master.id()
        );
        return new RuleGatewayBinding(
                gatewayKey,
                master.ruleName(),
                null,
                Map.of(
                        "targetType", master.targetType().name(),
                        "targetMasterId", master.id(),
                        "targetCode", master.code()
                )
        );
    }
}
