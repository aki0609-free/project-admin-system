package com.project.backend.features.system.rule.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RuleGatewayContractRegistryTest {

    @Test
    void resolvesDynamicContractWhenNoStaticContractExists() {
        RuleGatewayDynamicContractProvider dynamicProvider =
                new RuleGatewayDynamicContractProvider() {
                    @Override
                    public boolean supports(String gatewayKey) {
                        return gatewayKey.startsWith("DYNAMIC.");
                    }

                    @Override
                    public RuleGatewayContract contract(String gatewayKey) {
                        return new RuleGatewayContract(
                                gatewayKey,
                                RuleGatewayResultShape.SCALAR,
                                Set.of("amount"),
                                true
                        );
                    }
                };
        RuleGatewayContractRegistry registry =
                new RuleGatewayContractRegistry(
                        List.of(),
                        List.of(dynamicProvider)
                );

        RuleGatewayContract result = registry.require("DYNAMIC.42");

        assertThat(result.gatewayKey()).isEqualTo("DYNAMIC.42");
        assertThat(result.requiredParameters()).containsExactly("amount");
    }

    @Test
    void rejectsUnknownDynamicKey() {
        RuleGatewayContractRegistry registry =
                new RuleGatewayContractRegistry(List.of(), List.of());

        assertThatThrownBy(() -> registry.require("UNKNOWN.42"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未登録");
    }
}
