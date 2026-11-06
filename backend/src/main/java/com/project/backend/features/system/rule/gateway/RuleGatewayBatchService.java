package com.project.backend.features.system.rule.gateway;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchCalculationRequest;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchBindingResponse;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchErrorResponse;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchRequest;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchResponse;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchRowRequest;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayBatchRowResponse;
import com.project.backend.features.system.rule.gateway.dto.RuleGatewayFailureMode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleGatewayBatchService {

    private final RuleGatewayService gatewayService;

    @Transactional(readOnly = true)
    public RuleGatewayBatchResponse executeFrontend(
            RuleGatewayBatchRequest request
    ) {
        validateUniqueKeys(request);
        RuleGatewayFailureMode failureMode = request.failureMode() == null
                ? RuleGatewayFailureMode.FAIL_FAST
                : request.failureMode();
        Set<String> gatewayKeys = request.calculations().stream()
                .map(RuleGatewayBatchCalculationRequest::gatewayKey)
                .collect(java.util.stream.Collectors.toCollection(
                        java.util.LinkedHashSet::new
                ));
        Map<String, RuleGatewayPreparedBinding> preparedBindings =
                gatewayService.prepareFrontendBindings(
                        gatewayKeys,
                        request.targetDate()
                );
        List<RuleGatewayBatchBindingResponse> bindingResponses =
                request.calculations().stream()
                        .map(calculation -> {
                            RuleGatewayBinding binding = preparedBindings.get(
                                    calculation.gatewayKey()
                            ).binding();
                            return RuleGatewayBatchBindingResponse.from(
                                    calculation.outputKey(),
                                    binding,
                                    gatewayService.requireContract(
                                            calculation.gatewayKey()
                                    )
                            );
                        })
                        .toList();
        List<RuleGatewayBatchRowResponse> rows = request.rows().stream()
                .map(row -> executeRow(
                        request,
                        row,
                        failureMode,
                        preparedBindings
                ))
                .toList();
        return new RuleGatewayBatchResponse(bindingResponses, rows);
    }

    private RuleGatewayBatchRowResponse executeRow(
            RuleGatewayBatchRequest request,
            RuleGatewayBatchRowRequest row,
            RuleGatewayFailureMode failureMode,
            Map<String, RuleGatewayPreparedBinding> preparedBindings
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        Map<String, RuleGatewayBatchErrorResponse> errors =
                new LinkedHashMap<>();
        for (RuleGatewayBatchCalculationRequest calculation
                : request.calculations()) {
            try {
                Map<String, Object> parameters = mergeParameters(
                        row.parameters(),
                        calculation.parameters()
                );
                RuleGatewayExecution execution =
                        gatewayService.executeFrontendPrepared(
                                preparedBindings.get(
                                        calculation.gatewayKey()
                                ),
                                request.targetDate(),
                                parameters
                        );
                values.put(calculation.outputKey(), execution.result());
            } catch (RuntimeException exception) {
                if (failureMode == RuleGatewayFailureMode.FAIL_FAST) {
                    throw exception;
                }
                errors.put(
                        calculation.outputKey(),
                        new RuleGatewayBatchErrorResponse(
                                "RULE_CALCULATION_FAILED",
                                "計算に失敗しました。"
                        )
                );
            }
        }
        return new RuleGatewayBatchRowResponse(
                row.rowKey(),
                Map.copyOf(values),
                Map.copyOf(errors)
        );
    }

    private Map<String, Object> mergeParameters(
            Map<String, Object> rowParameters,
            Map<String, Object> calculationParameters
    ) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (rowParameters != null) merged.putAll(rowParameters);
        if (calculationParameters != null) {
            merged.putAll(calculationParameters);
        }
        return merged;
    }

    private void validateUniqueKeys(RuleGatewayBatchRequest request) {
        requireUnique(
                request.rows().stream()
                        .map(RuleGatewayBatchRowRequest::rowKey)
                        .toList(),
                "rowKey"
        );
        requireUnique(
                request.calculations().stream()
                        .map(RuleGatewayBatchCalculationRequest::outputKey)
                        .toList(),
                "outputKey"
        );
    }

    private void requireUnique(List<String> values, String label) {
        Set<String> unique = new HashSet<>();
        for (String value : values) {
            if (!unique.add(value)) {
                throw new IllegalArgumentException(
                        label + "が重複しています。value=" + value
                );
            }
        }
    }
}
