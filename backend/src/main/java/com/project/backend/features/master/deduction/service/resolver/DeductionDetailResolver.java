package com.project.backend.features.master.deduction.service.resolver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.project.backend.features.master.deduction.dto.BaseDeductionDetailResponse;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;
import com.project.backend.features.master.deduction.provider.DeductionDetailProvider;

@Component
public class DeductionDetailResolver {

    private final Map<DeductionDetailViewType, DeductionDetailProvider> providerMap;

    @SuppressWarnings("null")
public DeductionDetailResolver(
            List<DeductionDetailProvider> providers
    ) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        DeductionDetailProvider::supports,
                        Function.identity()
                ));
    }

    public Map<String, List<BaseDeductionDetailResponse>> resolve(
            DeductionMaster deduction
    ) {

        DeductionDetailViewType viewType =
                deduction.getDetailViewType();

        if (viewType == null || viewType == DeductionDetailViewType.NONE) {
            return Collections.emptyMap();
        }

        DeductionDetailProvider provider =
                providerMap.get(viewType);

        if (provider == null) {
            return Collections.emptyMap();
        }

        return Map.of(
                viewType.name(),
                provider.getDetails(deduction)
        );
    }
}