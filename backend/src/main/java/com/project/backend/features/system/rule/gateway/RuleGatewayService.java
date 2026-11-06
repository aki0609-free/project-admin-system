package com.project.backend.features.system.rule.gateway;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.system.rule.dto.RuleContextRequest;
import com.project.backend.features.system.rule.dto.RuleExecutionResult;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.service.RuleExecutionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleGatewayService {

    private final RuleGatewayContractRegistry contractRegistry;
    private final RuleGatewayBindingResolverRegistry bindingResolverRegistry;
    private final RuleExecutionService executionService;
    private final ObjectMapper objectMapper;

    public RuleGatewayExecution execute(
            String gatewayKey,
            LocalDate targetDate,
            Map<String, Object> parameters
    ) {
        RuleGatewayBinding binding = bindingResolverRegistry.resolve(
                gatewayKey,
                targetDate
        );
        return executeBound(
                binding,
                withTargetDate(parameters, targetDate)
        );
    }

    public RuleGatewayExecution executeFrontend(
            String gatewayKey,
            LocalDate targetDate,
            Map<String, Object> parameters
    ) {
        requireFrontendContract(gatewayKey);
        return execute(gatewayKey, targetDate, parameters);
    }

    public Map<String, RuleGatewayBinding> resolveFrontendBindings(
            Set<String> gatewayKeys,
            LocalDate targetDate
    ) {
        for (String gatewayKey : gatewayKeys) {
            requireFrontendContract(gatewayKey);
        }
        return bindingResolverRegistry.resolveAll(gatewayKeys, targetDate);
    }

    public Map<String, RuleGatewayPreparedBinding> prepareFrontendBindings(
            Set<String> gatewayKeys,
            LocalDate targetDate
    ) {
        Map<String, RuleGatewayBinding> bindings = resolveFrontendBindings(
                gatewayKeys,
                targetDate
        );
        return prepareBindings(bindings.values());
    }

    public Map<String, RuleGatewayPreparedBinding> prepareBindings(
            Collection<RuleGatewayBinding> bindings
    ) {
        Map<String, RuleGatewayBinding> bindingsByKey = new LinkedHashMap<>();
        for (RuleGatewayBinding binding : bindings) {
            contractRegistry.require(binding.gatewayKey());
            RuleGatewayBinding duplicate = bindingsByKey.putIfAbsent(
                    binding.gatewayKey(),
                    binding
            );
            if (duplicate != null && !duplicate.equals(binding)) {
                throw new IllegalArgumentException(
                        "同じgatewayKeyに複数のRule割当があります。gatewayKey="
                                + binding.gatewayKey()
                );
            }
        }
        Set<String> ruleNames = bindingsByKey.values().stream()
                .map(RuleGatewayBinding::ruleName)
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));
        Map<String, RuleMaster> rules = executionService.loadActiveRules(
                ruleNames
        );
        Map<String, RuleGatewayPreparedBinding> result =
                new LinkedHashMap<>();
        bindingsByKey.forEach((gatewayKey, binding) -> result.put(
                gatewayKey,
                new RuleGatewayPreparedBinding(
                        binding,
                        rules.get(binding.ruleName())
                )
        ));
        return Map.copyOf(result);
    }

    public RuleGatewayContract requireContract(String gatewayKey) {
        return contractRegistry.require(gatewayKey);
    }

    public RuleGatewayExecution executeFrontendBound(
            RuleGatewayBinding binding,
            LocalDate targetDate,
            Map<String, Object> parameters
    ) {
        requireFrontendContract(binding.gatewayKey());
        return executeBound(
                binding,
                withTargetDate(parameters, targetDate)
        );
    }

    public RuleGatewayExecution executeFrontendPrepared(
            RuleGatewayPreparedBinding prepared,
            LocalDate targetDate,
            Map<String, Object> parameters
    ) {
        RuleGatewayBinding binding = prepared.binding();
        requireFrontendContract(binding.gatewayKey());
        return executePrepared(
                prepared,
                withTargetDate(parameters, targetDate)
        );
    }

    public RuleGatewayExecution executePrepared(
            RuleGatewayPreparedBinding prepared,
            Map<String, Object> parameters
    ) {
        RuleGatewayBinding binding = prepared.binding();
        RuleGatewayContract contract = contractRegistry.require(
                binding.gatewayKey()
        );
        Map<String, Object> safeParameters = mergeBindingParameters(
                binding,
                parameters == null ? Map.of() : parameters
        );
        validateRequiredParameters(contract, safeParameters);
        RuleExecutionResult result = executionService.executeLoaded(
                prepared.rule(),
                RuleContextRequest.builder()
                        .parameters(safeParameters)
                        .build()
        );
        return toExecution(binding, contract, result);
    }

    public RuleGatewayExecution executeBound(
            RuleGatewayBinding binding,
            Map<String, Object> parameters
    ) {
        RuleGatewayContract contract = contractRegistry.require(
                binding.gatewayKey()
        );
        Map<String, Object> safeParameters = parameters == null
                ? Map.of()
                : new LinkedHashMap<>(parameters);
        safeParameters = mergeBindingParameters(binding, safeParameters);
        validateRequiredParameters(contract, safeParameters);

        RuleExecutionResult result = executionService.execute(
                binding.ruleName(),
                RuleContextRequest.builder()
                        .parameters(safeParameters)
                        .build()
        );
        return toExecution(binding, contract, result);
    }

    public <T> T convertResult(
            RuleGatewayExecution execution,
            Class<T> resultType
    ) {
        return objectMapper.convertValue(execution.result(), resultType);
    }

    private void validateRequiredParameters(
            RuleGatewayContract contract,
            Map<String, Object> parameters
    ) {
        for (String required : contract.requiredParameters()) {
            if (!parameters.containsKey(required)
                    || parameters.get(required) == null) {
                throw new IllegalArgumentException(
                        "Rule Gateway必須パラメータがありません。gatewayKey="
                                + contract.gatewayKey()
                                + ", parameter="
                                + required
                );
            }
        }
    }

    private void validateResultShape(
            RuleGatewayContract contract,
            Object result
    ) {
        if (result == null) {
            throw new IllegalStateException(
                    "Rule Gatewayの計算結果がnullです。gatewayKey="
                            + contract.gatewayKey()
            );
        }
        boolean collectionLike = result instanceof Collection<?>
                || result.getClass().isArray();
        boolean mapLike = result instanceof Map<?, ?>;
        boolean valid = switch (contract.resultShape()) {
            case SCALAR -> !collectionLike && !mapLike;
            case OBJECT -> mapLike || !isSimpleValue(result);
            case LIST, TABLE -> collectionLike;
        };
        if (!valid) {
            throw new IllegalStateException(
                    "Rule Gatewayの結果形式が契約と一致しません。gatewayKey="
                            + contract.gatewayKey()
                            + ", expected="
                            + contract.resultShape()
            );
        }
    }

    private RuleGatewayExecution toExecution(
            RuleGatewayBinding binding,
            RuleGatewayContract contract,
            RuleExecutionResult result
    ) {
        validateResultShape(contract, result.result());
        return new RuleGatewayExecution(
                binding.gatewayKey(),
                binding.ruleName(),
                binding.ruleVersion(),
                contract.resultShape(),
                result.result(),
                result.facts() == null ? Map.of() : result.facts()
        );
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value.getClass().isEnum()
                || value.getClass().isArray() && Array.getLength(value) == 0;
    }

    private RuleGatewayContract requireFrontendContract(String gatewayKey) {
        RuleGatewayContract contract = contractRegistry.require(gatewayKey);
        if (!contract.frontendExecutable()) {
            throw new IllegalArgumentException(
                    "フロント実行が許可されていないRule Gatewayです。gatewayKey="
                            + gatewayKey
            );
        }
        return contract;
    }

    private Map<String, Object> withTargetDate(
            Map<String, Object> parameters,
            LocalDate targetDate
    ) {
        Map<String, Object> result = parameters == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(parameters);
        if (targetDate != null) {
            result.putIfAbsent("targetDate", targetDate);
        }
        return result;
    }

    private Map<String, Object> mergeBindingParameters(
            RuleGatewayBinding binding,
            Map<String, Object> parameters
    ) {
        Map<String, Object> result = new LinkedHashMap<>(parameters);
        result.putAll(binding.parameters());
        return result;
    }
}
