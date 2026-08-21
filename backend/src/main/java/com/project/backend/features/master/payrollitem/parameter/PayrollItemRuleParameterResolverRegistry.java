package com.project.backend.features.master.payrollitem.parameter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class PayrollItemRuleParameterResolverRegistry {

    private final Map<String, PayrollItemRuleParameterValueResolver> resolvers;

    public PayrollItemRuleParameterResolverRegistry(
            List<PayrollItemRuleParameterValueResolver> resolvers
    ) {
        this.resolvers = resolvers.stream().collect(Collectors.toUnmodifiableMap(
                PayrollItemRuleParameterValueResolver::key,
                Function.identity()
        ));
    }

    public Object resolve(
            String resolverKey,
            PayrollItemRuleParameterResolutionContext context
    ) {
        PayrollItemRuleParameterValueResolver resolver = resolvers.get(resolverKey);
        if (resolver == null) {
            throw new IllegalStateException(
                    "給与項目パラメーターResolverが見つかりません。key="
                            + resolverKey
            );
        }
        return resolver.resolve(context);
    }
}
