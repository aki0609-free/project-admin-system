package com.project.backend.features.system.rule.gateway;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class RuleGatewayContractRegistry {

    private final Map<String, RuleGatewayContract> contracts;
    private final List<RuleGatewayDynamicContractProvider> dynamicProviders;

    public RuleGatewayContractRegistry(
            List<RuleGatewayContractProvider> providers,
            List<RuleGatewayDynamicContractProvider> dynamicProviders
    ) {
        Map<String, RuleGatewayContract> collected = new LinkedHashMap<>();
        for (RuleGatewayContractProvider provider : providers) {
            for (RuleGatewayContract contract : provider.contracts()) {
                RuleGatewayContract duplicate = collected.putIfAbsent(
                        contract.gatewayKey(),
                        contract
                );
                if (duplicate != null) {
                    throw new IllegalStateException(
                            "Rule Gateway契約が重複しています。gatewayKey="
                                    + contract.gatewayKey()
                    );
                }
            }
        }
        this.contracts = Map.copyOf(collected);
        this.dynamicProviders = List.copyOf(dynamicProviders);
    }

    public RuleGatewayContract require(String gatewayKey) {
        RuleGatewayContract contract = contracts.get(gatewayKey);
        if (contract != null) {
            return contract;
        }

        List<RuleGatewayDynamicContractProvider> matched =
                dynamicProviders.stream()
                        .filter(provider -> provider.supports(gatewayKey))
                        .toList();
        if (matched.size() == 1) {
            return matched.getFirst().contract(gatewayKey);
        }
        if (matched.size() > 1) {
            throw new IllegalStateException(
                    "動的Rule Gateway契約が重複しています。gatewayKey="
                            + gatewayKey
            );
        }
        throw new IllegalArgumentException(
                "未登録のRule Gateway Keyです。gatewayKey=" + gatewayKey
        );
    }
}
