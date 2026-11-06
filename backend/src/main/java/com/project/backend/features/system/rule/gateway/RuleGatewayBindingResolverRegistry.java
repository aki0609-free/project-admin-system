package com.project.backend.features.system.rule.gateway;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class RuleGatewayBindingResolverRegistry {

    private final List<RuleGatewayBindingResolver> resolvers;

    public RuleGatewayBindingResolverRegistry(
            List<RuleGatewayBindingResolver> resolvers
    ) {
        this.resolvers = List.copyOf(resolvers);
    }

    public RuleGatewayBinding resolve(
            String gatewayKey,
            LocalDate targetDate
    ) {
        return requireResolver(gatewayKey).resolve(gatewayKey, targetDate);
    }

    public Map<String, RuleGatewayBinding> resolveAll(
            Set<String> gatewayKeys,
            LocalDate targetDate
    ) {
        Map<RuleGatewayBindingResolver, Set<String>> grouped =
                new LinkedHashMap<>();
        for (String gatewayKey : gatewayKeys) {
            RuleGatewayBindingResolver resolver = requireResolver(gatewayKey);
            grouped.computeIfAbsent(
                    resolver,
                    ignored -> new LinkedHashSet<>()
            ).add(gatewayKey);
        }

        Map<String, RuleGatewayBinding> result = new LinkedHashMap<>();
        grouped.forEach((resolver, keys) ->
                result.putAll(resolver.resolveAll(keys, targetDate))
        );
        for (String gatewayKey : gatewayKeys) {
            if (!result.containsKey(gatewayKey)) {
                throw new IllegalStateException(
                        "Rule Gateway割当を一括解決できませんでした。gatewayKey="
                                + gatewayKey
                );
            }
        }
        return Map.copyOf(result);
    }

    private RuleGatewayBindingResolver requireResolver(String gatewayKey) {
        List<RuleGatewayBindingResolver> matched = resolvers.stream()
                .filter(resolver -> resolver.supports(gatewayKey))
                .toList();
        if (matched.size() != 1) {
            throw new IllegalStateException(
                    "Rule Gateway割当Resolverは1件必要です。gatewayKey="
                            + gatewayKey
                            + ", count="
                            + matched.size()
            );
        }
        return matched.getFirst();
    }
}
