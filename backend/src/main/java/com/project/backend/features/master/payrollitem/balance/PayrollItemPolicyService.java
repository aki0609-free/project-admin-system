package com.project.backend.features.master.payrollitem.balance;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.employee.dto.PayrollItemParameterDefinitionResponse;
import com.project.backend.features.employee.dto.PayrollItemParameterOptionResponse;
import com.project.backend.features.master.payrollitem.enums.PayrollItemInputSource;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayrollItemPolicyService {

    private final PayrollItemBalancePolicyRepository policyRepository;
    private final PayrollItemParameterDefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public void synchronize(
            PayrollItemTargetType targetType,
            Long masterId,
            String code,
            String displayName,
            PayrollItemPolicySaveRequest request
    ) {
        if (request == null) return;
        validate(request);
        String tenantId = TenantContext.getTenantId();
        PayrollItemBalancePolicy policy = policyRepository
                .findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                        tenantId, targetType, code)
                .orElseGet(PayrollItemBalancePolicy::new);
        policy.setTenantId(tenantId);
        policy.setTargetType(targetType);
        policy.setTargetMasterId(masterId);
        policy.setTargetCode(code);
        policy.setDisplayName(displayName);
        policy.setApplicationScope(request.applicationScope());
        policy.setInputSource(request.inputSource());
        policy.setBalanceTrackingFlag(request.balanceTracking());
        policy.setBalanceUnit(request.balanceUnit() == null
                ? BalanceUnit.AMOUNT : request.balanceUnit());
        policy.setAccrualFrequency(defaultText(request.accrualFrequency(), "MANUAL"));
        policy.setAccrualRuleName(defaultText(
                request.accrualRuleName(), "MANUAL_TRANSACTION"));
        policy.setCarryForwardFlag(request.carryForward());
        policy.setAdvanceConsumptionFlag(request.advanceConsumption());
        policy.setActiveFlag(true);
        policy.setDeletedAt(null);
        policy = policyRepository.save(policy);
        synchronizeDefinitions(policy, request.parameterDefinitions());
    }

    @Transactional(readOnly = true)
    public PayrollItemPolicyResponse find(
            PayrollItemTargetType targetType,
            Long masterId
    ) {
        return policyRepository
                .findByTenantIdAndTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        TenantContext.getTenantId(), targetType, masterId)
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    private void synchronizeDefinitions(
            PayrollItemBalancePolicy policy,
            List<PayrollItemParameterDefinitionSaveRequest> requests
    ) {
        List<PayrollItemParameterDefinitionSaveRequest> safeRequests =
                requests == null ? List.of() : requests;
        List<PayrollItemParameterDefinition> existing = definitionRepository
                .findAllByTenantIdAndBalancePolicyIdOrderByDisplayOrderAscIdAsc(
                        TenantContext.getTenantId(), policy.getId());
        var requestedKeys = safeRequests.stream()
                .map(item -> normalizeKey(item.key()))
                .collect(java.util.stream.Collectors.toSet());
        Instant now = Instant.now(clock);
        existing.stream()
                .filter(item -> !requestedKeys.contains(item.getParameterKey()))
                .forEach(item -> {
                    item.setActiveFlag(false);
                    item.setDeletedAt(now);
                });

        for (PayrollItemParameterDefinitionSaveRequest request : safeRequests) {
            String key = normalizeKey(request.key());
            PayrollItemParameterDefinition definition = existing.stream()
                    .filter(item -> item.getParameterKey().equals(key))
                    .findFirst()
                    .orElseGet(PayrollItemParameterDefinition::new);
            definition.setTenantId(TenantContext.getTenantId());
            definition.setBalancePolicyId(policy.getId());
            definition.setParameterKey(key);
            definition.setDisplayName(request.displayName().trim());
            definition.setInputType(request.inputType());
            definition.setRequiredFlag(request.required());
            definition.setDefaultValue(blankToNull(request.defaultValue()));
            definition.setOptionsJson(writeOptions(request.options()));
            definition.setRuleParameterFlag(request.ruleParameter());
            definition.setDailyDisplayFlag(request.dailyDisplay());
            definition.setInputSourceOverrideFlag(request.inputSourceOverride());
            definition.setRuleValueResolverKey(
                    blankToNull(request.ruleValueResolverKey()));
            definition.setDisplayOrder(request.displayOrder());
            definition.setActiveFlag(true);
            definition.setDeletedAt(null);
            definitionRepository.save(definition);
        }
    }

    private void validate(PayrollItemPolicySaveRequest request) {
        if (request.inputSource() == PayrollItemInputSource.TRANSACTION
                && request.applicationScope()
                != PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT) {
            throw new IllegalArgumentException(
                    "明細取引入力は従業員別適用を選択してください。"
            );
        }
        long sourceOverrides = request.parameterDefinitions() == null ? 0
                : request.parameterDefinitions().stream()
                .filter(PayrollItemParameterDefinitionSaveRequest::inputSourceOverride)
                .count();
        if (sourceOverrides > 1) {
            throw new IllegalArgumentException("入力元切替パラメーターは1件だけ設定できます。");
        }
        if (request.parameterDefinitions() == null) return;
        var definitionsByKey = new java.util.LinkedHashMap<
                String, PayrollItemParameterDefinitionSaveRequest>();
        for (PayrollItemParameterDefinitionSaveRequest definition
                : request.parameterDefinitions()) {
            if (definition == null) {
                throw new IllegalArgumentException("パラメーター定義が不正です。");
            }
            String key = normalizeKey(definition.key());
            if (definitionsByKey.putIfAbsent(key, definition) != null) {
                throw new IllegalArgumentException(
                        "パラメーターキーが重複しています。key=" + key);
            }
            if (definition.displayName() == null
                    || definition.displayName().isBlank()) {
                throw new IllegalArgumentException(
                        "パラメーターの表示名は必須です。key=" + key);
            }
            if (definition.inputType() == PayrollItemParameterInputType.SELECT
                    && (definition.options() == null || definition.options().isEmpty())) {
                throw new IllegalArgumentException(
                        definition.displayName() + " の選択肢を設定してください。"
                );
            }
            validateOptions(definition);
            validateDefaultValue(definition);
            if (definition.inputSourceOverride()) {
                if (definition.inputType() != PayrollItemParameterInputType.SELECT) {
                    throw new IllegalArgumentException(
                            "入力元切替パラメーターはSELECT型にしてください。"
                    );
                }
                boolean invalidOption = definition.options() == null
                        || definition.options().stream()
                        .map(PayrollItemParameterOption::value)
                        .anyMatch(value -> !PayrollItemInputSource.DAILY_REPORT.name().equals(value)
                                && !PayrollItemInputSource.TRANSACTION.name().equals(value));
                if (invalidOption) {
                    throw new IllegalArgumentException(
                            "入力元切替の値はDAILY_REPORTまたはTRANSACTIONにしてください。"
                    );
                }
            }
        }
        definitionsByKey.forEach((key, definition) ->
                validateResolver(key, definition, definitionsByKey));
    }

    private void validateOptions(
            PayrollItemParameterDefinitionSaveRequest definition
    ) {
        if (definition.inputType() != PayrollItemParameterInputType.SELECT) {
            return;
        }
        var values = new java.util.HashSet<String>();
        for (PayrollItemParameterOption option : definition.options()) {
            if (option == null || option.label() == null
                    || option.label().isBlank() || option.value() == null
                    || option.value().isBlank()) {
                throw new IllegalArgumentException(
                        definition.displayName()
                                + " の選択肢は表示名と値を入力してください。"
                );
            }
            if (!values.add(option.value())) {
                throw new IllegalArgumentException(
                        definition.displayName()
                                + " の選択肢の値が重複しています。value="
                                + option.value()
                );
            }
        }
    }

    private void validateDefaultValue(
            PayrollItemParameterDefinitionSaveRequest definition
    ) {
        String value = definition.defaultValue();
        if (value == null || value.isBlank()) return;
        try {
            switch (definition.inputType()) {
                case NUMBER -> new java.math.BigDecimal(value);
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(value)
                            && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException();
                    }
                }
                case DATE -> java.time.LocalDate.parse(value);
                case SELECT -> {
                    if (definition.options().stream()
                            .noneMatch(option -> option.value().equals(value))) {
                        throw new IllegalArgumentException();
                    }
                }
                case TEXT -> { }
            }
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    definition.displayName() + " の初期値が不正です。",
                    exception
            );
        }
    }

    private void validateResolver(
            String parameterKey,
            PayrollItemParameterDefinitionSaveRequest definition,
            java.util.Map<String, PayrollItemParameterDefinitionSaveRequest>
                    definitionsByKey
    ) {
        String resolverKey = definition.ruleValueResolverKey();
        if (resolverKey == null || resolverKey.isBlank()) return;
        if (!definition.ruleParameter()) {
            throw new IllegalArgumentException(
                    "Rule値Resolverを指定した項目はRuleへ渡す設定を有効にしてください。key="
                            + parameterKey
            );
        }
        String prefix = "SELECT_OPTION_CALCULATION_VALUE:";
        if (!resolverKey.startsWith(prefix)
                || resolverKey.length() == prefix.length()) {
            throw new IllegalArgumentException(
                    "未対応のRule値Resolverです。key=" + resolverKey
            );
        }
        String sourceKey = normalizeKey(resolverKey.substring(prefix.length()));
        PayrollItemParameterDefinitionSaveRequest source =
                definitionsByKey.get(sourceKey);
        if (source == null
                || source.inputType() != PayrollItemParameterInputType.SELECT) {
            throw new IllegalArgumentException(
                    "選択肢計算値Resolverの参照元は同じ設定内のSELECT項目にしてください。key="
                            + sourceKey
            );
        }
        if (definition.inputType() != PayrollItemParameterInputType.NUMBER) {
            throw new IllegalArgumentException(
                    "選択肢計算値Resolverの出力先はNUMBER項目にしてください。key="
                            + parameterKey
            );
        }
        if (source.options().stream()
                .anyMatch(option -> option.calculationValue() == null)) {
            throw new IllegalArgumentException(
                    source.displayName()
                            + " の全選択肢に計算値を設定してください。"
            );
        }
    }

    private PayrollItemPolicyResponse toResponse(PayrollItemBalancePolicy policy) {
        List<PayrollItemParameterDefinitionResponse> definitions = definitionRepository
                .findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        TenantContext.getTenantId(), policy.getId())
                .stream()
                .map(this::toDefinitionResponse)
                .toList();
        return new PayrollItemPolicyResponse(
                policy.getApplicationScope().name(), policy.getInputSource().name(),
                policy.isBalanceTrackingFlag(), policy.getBalanceUnit().name(),
                policy.getAccrualFrequency(), policy.getAccrualRuleName(),
                policy.isCarryForwardFlag(), policy.isAdvanceConsumptionFlag(),
                definitions);
    }

    private PayrollItemPolicyResponse defaultResponse() {
        return new PayrollItemPolicyResponse(
                PayrollItemApplicationScope.ALL_EMPLOYEES.name(),
                PayrollItemInputSource.DAILY_REPORT.name(), false,
                BalanceUnit.AMOUNT.name(), "MANUAL", "MANUAL_TRANSACTION",
                false, false, List.of());
    }

    private PayrollItemParameterDefinitionResponse toDefinitionResponse(
            PayrollItemParameterDefinition definition
    ) {
        return new PayrollItemParameterDefinitionResponse(
                definition.getParameterKey(), definition.getDisplayName(),
                definition.getInputType().name(), definition.isRequiredFlag(),
                definition.getDefaultValue(), readOptions(definition.getOptionsJson()),
                definition.isRuleParameterFlag(), definition.isDailyDisplayFlag(),
                definition.isInputSourceOverrideFlag(),
                definition.getRuleValueResolverKey(), definition.getDisplayOrder());
    }

    private List<PayrollItemParameterOptionResponse> readOptions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<PayrollItemParameterOptionResponse>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("給与項目の選択肢を読み込めません。", exception);
        }
    }

    private String writeOptions(List<PayrollItemParameterOption> options) {
        if (options == null || options.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception exception) {
            throw new IllegalArgumentException("給与項目の選択肢を保存できません。", exception);
        }
    }

    private String normalizeKey(String key) {
        String normalized = key == null ? "" : key.trim();
        if (!normalized.matches("[A-Za-z][A-Za-z0-9_]{0,99}")) {
            throw new IllegalArgumentException(
                    "パラメーターキーは英字で開始し、英数字と_で指定してください。"
            );
        }
        return normalized;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
