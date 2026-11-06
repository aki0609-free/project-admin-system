package com.project.backend.features.system.rule.gateway;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface RuleGatewayBindingResolver {

    boolean supports(String gatewayKey);

    RuleGatewayBinding resolve(String gatewayKey, LocalDate targetDate);

    default Map<String, RuleGatewayBinding> resolveAll(
            Set<String> gatewayKeys,
            LocalDate targetDate
    ) {
        Map<String, RuleGatewayBinding> result = new LinkedHashMap<>();
        for (String gatewayKey : gatewayKeys) {
            result.put(gatewayKey, resolve(gatewayKey, targetDate));
        }
        return Map.copyOf(result);
    }
}
