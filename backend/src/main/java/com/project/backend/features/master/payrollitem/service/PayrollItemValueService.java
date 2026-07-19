package com.project.backend.features.master.payrollitem.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.features.master.payrollitem.dto.PayrollItemMasterSnapshot;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueResult;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.provider.PayrollItemValueProvider;
import com.project.backend.features.system.rule.dto.RuleContextRequest;
import com.project.backend.features.system.rule.dto.RuleExecutionResult;
import com.project.backend.features.system.rule.service.RuleExecutionService;

@Service
@Transactional(readOnly = true)
public class PayrollItemValueService {

    private final Map<PayrollItemTargetType, PayrollItemValueProvider> providerMap;
    private final RuleExecutionService ruleExecutionService;

    @SuppressWarnings("null")
    public PayrollItemValueService(
            List<PayrollItemValueProvider> providers,
            RuleExecutionService ruleExecutionService
    ) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        PayrollItemValueProvider::supports,
                        Function.identity()
                ));

        this.ruleExecutionService = ruleExecutionService;
    }

    public PayrollItemValueResult calculate(
            PayrollItemValueRequest request
    ) {
        PayrollItemMasterSnapshot master = findMaster(request);

        RuleExecutionResult ruleResult = null;

        BigDecimal amount = switch (master.calculationType()) {
            case "MANUAL" -> manualAmount(request);
            case "FIXED" -> fixedAmount(master);
            case "AUTO" -> {
                ruleResult = executeRule(master, request);
                yield toBigDecimal(ruleResult.result());
            }
            default -> throw new IllegalArgumentException(
                    "未対応の calculationType です。 calculationType=" + master.calculationType()
            );
        };

        BigDecimal limitedAmount =
                applyLimit(
                        amount,
                        master.minAmount(),
                        master.maxAmount()
                );

        return new PayrollItemValueResult(
                request.targetType(),
                master.id(),
                master.code(),
                master.name(),
                master.calculationType(),
                master.ruleName(),
                limitedAmount,
                ruleResult != null && ruleResult.facts() != null
                        ? ruleResult.facts()
                        : buildBaseFacts(master, request)
        );
    }

    private PayrollItemMasterSnapshot findMaster(
            PayrollItemValueRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("PayrollItemValueRequest は必須です。");
        }

        if (request.targetType() == null) {
            throw new IllegalArgumentException("targetType は必須です。");
        }

        PayrollItemValueProvider provider =
                providerMap.get(request.targetType());

        if (provider == null) {
            throw new IllegalArgumentException(
                    "未対応の targetType です。 targetType=" + request.targetType()
            );
        }

        return provider.findMaster(request);
    }

    private BigDecimal manualAmount(
            PayrollItemValueRequest request
    ) {
        return request.manualAmount() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(request.manualAmount());
    }

    private BigDecimal fixedAmount(
            PayrollItemMasterSnapshot master
    ) {
        return master.defaultAmount() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(master.defaultAmount());
    }

    private RuleExecutionResult executeRule(
            PayrollItemMasterSnapshot master,
            PayrollItemValueRequest request
    ) {
        if (!StringUtils.hasText(master.ruleName())) {
            throw new IllegalStateException(
                    "AUTO計算ですが ruleName が設定されていません。 code=" + master.code()
            );
        }

        return ruleExecutionService.execute(
                master.ruleName(),
                RuleContextRequest.builder()
                        .parameters(buildRuleParameters(master, request))
                        .build()
        );
    }

    private Map<String, Object> buildRuleParameters(
            PayrollItemMasterSnapshot master,
            PayrollItemValueRequest request
    ) {
        Map<String, Object> parameters = new LinkedHashMap<>();

        if (request.parameters() != null) {
            parameters.putAll(request.parameters());
        }

        parameters.put("targetType", request.targetType().name());
        parameters.put("targetMasterId", master.id());
        parameters.put("targetCode", master.code());

        if (request.manualAmount() != null) {
            parameters.put("manualAmount", request.manualAmount());
        }

        return parameters;
    }

    private Map<String, Object> buildBaseFacts(
            PayrollItemMasterSnapshot master,
            PayrollItemValueRequest request
    ) {
        Map<String, Object> facts = new LinkedHashMap<>();

        if (request.parameters() != null) {
            facts.putAll(request.parameters());
        }

        facts.put("targetType", request.targetType().name());
        facts.put("targetMasterId", master.id());
        facts.put("targetCode", master.code());

        if (request.manualAmount() != null) {
            facts.put("manualAmount", request.manualAmount());
        }

        return facts;
    }

    private BigDecimal applyLimit(
            BigDecimal amount,
            Integer minAmount,
            Integer maxAmount
    ) {
        BigDecimal result = amount != null ? amount : BigDecimal.ZERO;

        if (minAmount != null) {
            BigDecimal min = BigDecimal.valueOf(minAmount);
            if (result.compareTo(min) < 0) {
                result = min;
            }
        }

        if (maxAmount != null) {
            BigDecimal max = BigDecimal.valueOf(maxAmount);
            if (result.compareTo(max) > 0) {
                result = max;
            }
        }

        return result;
    }

    private BigDecimal toBigDecimal(
            Object value
    ) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        return new BigDecimal(String.valueOf(value));
    }
}