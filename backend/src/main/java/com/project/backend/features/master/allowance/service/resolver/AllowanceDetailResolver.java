package com.project.backend.features.master.allowance.service.resolver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.project.backend.features.master.allowance.dto.BaseAllowanceDetailResponse;
import com.project.backend.features.master.allowance.entity.AllowanceMaster;
import com.project.backend.features.master.allowance.enums.AllowanceDetailViewType;
import com.project.backend.features.master.allowance.provider.AllowanceDetailProvider;

@Component
public class AllowanceDetailResolver {

    private final Map<AllowanceDetailViewType, AllowanceDetailProvider> providerMap;

    @SuppressWarnings("null")
    public AllowanceDetailResolver(
            List<AllowanceDetailProvider> providers
    ) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        AllowanceDetailProvider::supports,
                        Function.identity()
                ));
    }

    public Map<String, List<BaseAllowanceDetailResponse>> resolve(
            AllowanceMaster allowance
    ) {
        AllowanceDetailViewType viewType = allowance.getDetailViewType();

        if (viewType == null || viewType == AllowanceDetailViewType.NONE) {
            return Collections.emptyMap();
        }

        AllowanceDetailProvider provider = providerMap.get(viewType);

        if (provider == null) {
            return Collections.emptyMap();
        }

        return Map.of(
                viewType.name(),
                provider.getDetails(allowance)
        );
    }
}