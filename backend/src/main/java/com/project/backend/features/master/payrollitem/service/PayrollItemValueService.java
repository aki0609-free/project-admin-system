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
    private final PayrollMoneyPolicy moneyPolicy;

    @SuppressWarnings("null")
    public PayrollItemValueService(
            List<PayrollItemValueProvider> providers,
            RuleExecutionService ruleExecutionService,
            PayrollMoneyPolicy moneyPolicy
    ) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        PayrollItemValueProvider::supports,
                        Function.identity()
                ));

        this.ruleExecutionService = ruleExecutionService;
        this.moneyPolicy = moneyPolicy;
    }

    public PayrollItemValueResult calculate(
            PayrollItemValueRequest request
    ) {
        PayrollItemMasterSnapshot master = findMaster(request);

        RuleExecutionResult ruleResult = null;

        BigDecimal amount = switch (master.calculationType()) {
            case "MANUAL" -> manualAmount(master, request);
            case "FIXED" -> fixedAmount(master);
            case "AUTO" -> {
                ruleResult = executeRule(master, request);
                yield moneyPolicy.toDecimal(
                        ruleResult.result(),
                        "給与項目Rule計算結果"
                );
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
            PayrollItemMasterSnapshot master,
            PayrollItemValueRequest request
    ) {
        if (!Boolean.TRUE.equals(master.allowManualInput())) {
            throw new IllegalStateException(
                    "手入力が許可されていません。code=" + master.code()
            );
        }
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

        try {
            return ruleExecutionService.execute(
                    master.ruleName(),
                    RuleContextRequest.builder()
                            .parameters(buildRuleParameters(master, request))
                            .build()
            );
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "給与項目のRule計算に失敗しました。code="
                            + master.code()
                            + ", ruleName="
                            + master.ruleName(),
                    exception
            );
        }
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
        BigDecimal result = moneyPolicy.requireNonNegative(
                amount,
                "給与項目の計算結果"
        );

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

}
