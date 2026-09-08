package com.project.backend.features.master.payrollitem.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.master.payrollitem.dto.PayrollItemCalculationRequest;
import com.project.backend.features.master.payrollitem.dto.PayrollItemCalculationResult;
import com.project.backend.features.master.payrollitem.dto.PayrollItemMasterSnapshot;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollItemCalculationService {

    private final PayrollItemQueryService payrollItemQueryService;
    private final PayrollItemValueService payrollItemValueService;
    private final PayrollMoneyPolicy moneyPolicy;

    @SuppressWarnings("null")
    public List<PayrollItemCalculationResult> calculate(
            PayrollItemCalculationRequest request
    ) {
        return calculate(request, Map.of());
    }

    public List<PayrollItemCalculationResult> calculate(
            PayrollItemCalculationRequest request,
            Map<Long, Integer> manualAmounts
    ) {
        return calculate(
                request,
                manualAmounts,
                manualAmounts == null ? Set.of() : manualAmounts.keySet()
        );
    }

    public List<PayrollItemCalculationResult> calculate(
            PayrollItemCalculationRequest request,
            Map<Long, Integer> submittedAmounts,
            Set<Long> manualOverrideMasterIds
    ) {
        if (request == null) {
            throw new IllegalArgumentException("PayrollItemCalculationRequest は必須です。");
        }

        if (request.queryType() == null) {
            throw new IllegalArgumentException("queryType は必須です。");
        }

        List<PayrollItemMasterSnapshot> snapshots =
                request.targetType() == null
                        ? payrollItemQueryService.findItems(request.queryType())
                        : payrollItemQueryService.findItems(
                                request.queryType(),
                                request.targetType()
                        );

        return snapshots.stream()
                .filter(snapshot -> request.excludedMasterIds() == null
                        || !request.excludedMasterIds().contains(snapshot.id()))
                .map(snapshot -> calculateOne(
                        snapshot,
                        mergeParameters(
                                request.parameters(),
                                request.itemParameters() == null
                                        ? null
                                        : request.itemParameters().get(snapshot.id())
                        ),
                        submittedAmounts,
                        manualOverrideMasterIds
                ))
                .sorted(Comparator.comparing(
                        PayrollItemCalculationResult::displayOrder,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .toList();
    }

    private Map<String, Object> mergeParameters(
            Map<String, Object> common,
            Map<String, Object> itemSpecific
    ) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (common != null) merged.putAll(common);
        if (itemSpecific != null) merged.putAll(itemSpecific);
        return merged;
    }

    private PayrollItemCalculationResult calculateOne(
            PayrollItemMasterSnapshot snapshot,
            Map<String, Object> parameters,
            Map<Long, Integer> submittedAmounts,
            Set<Long> manualOverrideMasterIds
    ) {
        PayrollItemValueResult valueResult =
                payrollItemValueService.calculate(
                        new PayrollItemValueRequest(
                                snapshot.targetType(),
                                snapshot.id(),
                                snapshot.code(),
                                resolveManualAmount(
                                        snapshot,
                                        submittedAmounts,
                                        manualOverrideMasterIds
                                ),
                                parameters
                        )
                );

        BigDecimal calculatedAmount =
                valueResult.amount() == null
                        ? BigDecimal.ZERO
                        : moneyPolicy.roundToYen(
                                valueResult.amount()
                        );

        boolean manualOverride = isBaselineCalculation(valueResult.calculationType())
                && Boolean.TRUE.equals(snapshot.allowManualInput())
                && submittedAmounts != null
                && submittedAmounts.containsKey(snapshot.id())
                && manualOverrideMasterIds != null
                && manualOverrideMasterIds.contains(snapshot.id());
        BigDecimal amount = manualOverride
                ? applyLimits(
                        BigDecimal.valueOf(submittedAmounts.get(snapshot.id())),
                        snapshot.minAmount(), snapshot.maxAmount()
                )
                : calculatedAmount;

        return new PayrollItemCalculationResult(
                valueResult.targetType(),
                valueResult.targetMasterId(),
                valueResult.targetCode(),
                valueResult.targetName(),
                valueResult.calculationType(),
                valueResult.ruleName(),
                calculatedAmount,
                amount,
                manualOverride,
                snapshot.allowManualInput(),
                resolveDisplayOrder(snapshot.displayOrder()),
                buildFacts(valueResult.facts(), snapshot)
        );
    }

    private boolean isBaselineCalculation(String calculationType) {
        return "AUTO".equals(calculationType) || "FIXED".equals(calculationType);
    }

    private Integer resolveManualAmount(
            PayrollItemMasterSnapshot snapshot,
            Map<Long, Integer> submittedAmounts,
            Set<Long> manualOverrideMasterIds
    ) {
        if (Boolean.TRUE.equals(snapshot.allowManualInput())) {
            boolean manualItem = "MANUAL".equals(snapshot.calculationType());
            boolean explicitOverride = manualOverrideMasterIds != null
                    && manualOverrideMasterIds.contains(snapshot.id());
            if ((manualItem || explicitOverride)
                    && submittedAmounts != null
                    && submittedAmounts.containsKey(snapshot.id())) {
                return submittedAmounts.get(snapshot.id());
            }
            return snapshot.defaultAmount();
        }

        return null;
    }

    private Integer resolveDisplayOrder(
            Integer displayOrder
    ) {
        return displayOrder != null ? displayOrder : 9999;
    }

    private BigDecimal applyLimits(
            BigDecimal amount,
            Integer minAmount,
            Integer maxAmount
    ) {
        BigDecimal result = moneyPolicy.requireNonNegative(
                amount,
                "給与項目の手動変更額"
        );
        if (minAmount != null) {
            result = result.max(BigDecimal.valueOf(minAmount));
        }
        if (maxAmount != null) {
            result = result.min(BigDecimal.valueOf(maxAmount));
        }
        return result;
    }

    private Map<String, Object> buildFacts(
            Map<String, Object> facts,
            PayrollItemMasterSnapshot snapshot
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (facts != null) {
            result.putAll(facts);
        }

        result.put("displayOrder", resolveDisplayOrder(snapshot.displayOrder()));
        result.put("allowManualInput", Boolean.TRUE.equals(snapshot.allowManualInput()));

        return result;
    }
}
