package com.project.backend.features.master.payrollitem.balance;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.parameter.PayrollItemRuleParameterResolutionContext;
import com.project.backend.features.master.payrollitem.parameter.PayrollItemRuleParameterResolverRegistry;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayrollItemEnrollmentService {

    private final PayrollItemBalancePolicyRepository policyRepository;
    private final EmployeePayrollItemEnrollmentRepository enrollmentRepository;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final PayrollItemParameterDefinitionRepository parameterDefinitionRepository;
    private final PayrollItemRuleParameterResolverRegistry parameterResolverRegistry;

    @Transactional
    public void synchronize(
            Long employeeId,
            PayrollItemTargetType targetType,
            String targetCode,
            boolean enabled
    ) {
        synchronize(employeeId, targetType, targetCode, enabled, Map.of());
    }

    @Transactional
    public void synchronize(
            Long employeeId, PayrollItemTargetType targetType, String targetCode,
            boolean enabled, Map<String, String> parameters
    ) {
        synchronize(
                employeeId,
                targetType,
                targetCode,
                enabled,
                parameters,
                LocalDate.now(clock)
        );
    }

    @Transactional
    public void synchronize(
            Long employeeId, PayrollItemTargetType targetType, String targetCode,
            boolean enabled, Map<String, String> parameters,
            LocalDate effectiveFrom
    ) {
        var policy = policyRepository
                .findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                        TenantContext.getTenantId(), targetType, targetCode)
                .orElse(null);
        if (policy == null) {
            throw new IllegalArgumentException(
                    "手当・控除ポリシーが見つかりません。targetType="
                            + targetType + ", targetCode=" + targetCode
            );
        }
        if (policy.getApplicationScope() != PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT) {
            throw new IllegalArgumentException(
                    "全従業員対象の項目は従業員別に変更できません。targetCode=" + targetCode
            );
        }

        LocalDate operationDate = effectiveFrom == null
                ? LocalDate.now(clock)
                : effectiveFrom;
        Map<String, String> validatedParameters = enabled
                ? validateParameters(
                        employeeId, policy, parameters, operationDate)
                : Map.of();
        var current = enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        employeeId, policy.getId()
                );

        if (enabled && current.isEmpty()) {
            saveEnrollment(
                    employeeId,
                    policy.getId(),
                    operationDate,
                    validatedParameters
            );
        } else if (enabled) {
            updateEnabledEnrollment(
                    current.get(),
                    employeeId,
                    policy.getId(),
                    operationDate,
                    validatedParameters
            );
        } else if (!enabled && current.isPresent()) {
            closeEnrollment(current.get(), operationDate);
        }
    }

    private void updateEnabledEnrollment(
            EmployeePayrollItemEnrollment current,
            Long employeeId,
            Long policyId,
            LocalDate operationDate,
            Map<String, String> parameters
    ) {
        if (operationDate.isBefore(current.getEffectiveFrom())) {
            throw new IllegalArgumentException(
                    "適用開始日は現在の設定開始日以降で指定してください。"
            );
        }

        if (read(current.getSettingsJson()).equals(parameters)) {
            return;
        }

        if (operationDate.equals(current.getEffectiveFrom())) {
            current.setSettingsJson(write(parameters));
            return;
        }

        current.setEffectiveTo(operationDate.minusDays(1));
        saveEnrollment(
                employeeId,
                policyId,
                operationDate,
                parameters
        );
    }

    private void closeEnrollment(
            EmployeePayrollItemEnrollment current,
            LocalDate operationDate
    ) {
        if (operationDate.isBefore(current.getEffectiveFrom())) {
            throw new IllegalArgumentException(
                    "適用終了日は現在の設定開始日以降で指定してください。"
            );
        }

        if (operationDate.equals(current.getEffectiveFrom())) {
            current.setDeletedAt(Instant.now(clock));
            return;
        }

        // effectiveToは適用最終日（包含）。操作日から無効にする。
        current.setEffectiveTo(operationDate.minusDays(1));
    }

    private void saveEnrollment(
            Long employeeId,
            Long policyId,
            LocalDate effectiveFrom,
            Map<String, String> parameters
    ) {
        EmployeePayrollItemEnrollment enrollment =
                new EmployeePayrollItemEnrollment();
        enrollment.setEmployeeId(employeeId);
        enrollment.setBalancePolicyId(policyId);
        enrollment.setEffectiveFrom(effectiveFrom);
        enrollment.setSettingsJson(write(parameters));
        enrollmentRepository.save(enrollment);
    }

    private Map<String, String> validateParameters(
            Long employeeId,
            PayrollItemBalancePolicy policy,
            Map<String, String> parameters,
            LocalDate effectiveFrom
    ) {
        Map<String, String> values = parameters == null
                ? new java.util.LinkedHashMap<>()
                : new java.util.LinkedHashMap<>(parameters);
        List<PayrollItemParameterDefinition> definitions = parameterDefinitionRepository
                .findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        TenantContext.getTenantId(), policy.getId());

        var allowedKeys = definitions.stream()
                .map(PayrollItemParameterDefinition::getParameterKey)
                .collect(java.util.stream.Collectors.toSet());
        values.keySet().removeIf(key -> !allowedKeys.contains(key));

        for (PayrollItemParameterDefinition definition : definitions) {
            String value = values.get(definition.getParameterKey());
            if ((value == null || value.isBlank()) && definition.getDefaultValue() != null) {
                value = definition.getDefaultValue();
                values.put(definition.getParameterKey(), value);
            }
            if (definition.isRequiredFlag() && (value == null || value.isBlank())) {
                throw new IllegalArgumentException(
                        definition.getDisplayName() + " は必須です。"
                );
            }
            validateType(definition, value);
        }
        for (PayrollItemParameterDefinition definition : definitions) {
            if (definition.getRuleValueResolverKey() == null
                    || definition.getRuleValueResolverKey().isBlank()) {
                continue;
            }
            Object resolved = parameterResolverRegistry.resolve(
                    definition.getRuleValueResolverKey(),
                    new PayrollItemRuleParameterResolutionContext(
                            employeeId, effectiveFrom, values,
                            policy.getId(), null
                    )
            );
            String resolvedValue = toParameterText(resolved);
            validateType(definition, resolvedValue);
            values.put(definition.getParameterKey(), resolvedValue);
        }
        return values;
    }

    private String toParameterText(Object value) {
        if (value == null) return null;
        if (value instanceof java.math.BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private void validateType(PayrollItemParameterDefinition definition, String value) {
        if (value == null || value.isBlank()) return;
        try {
            switch (definition.getInputType()) {
                case NUMBER -> new java.math.BigDecimal(value);
                case BOOLEAN -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException();
                    }
                }
                case DATE -> java.time.LocalDate.parse(value);
                case SELECT -> validateSelectOption(definition, value);
                case TEXT -> { }
            }
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    definition.getDisplayName() + " の形式が不正です。", exception
            );
        }
    }

    private void validateSelectOption(
            PayrollItemParameterDefinition definition,
            String value
    ) {
        if (definition.getOptionsJson() == null
                || definition.getOptionsJson().isBlank()) {
            throw new IllegalArgumentException(
                    definition.getDisplayName() + " の選択肢が定義されていません。"
            );
        }
        try {
            List<PayrollItemParameterOption> options = objectMapper.readValue(
                    definition.getOptionsJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<
                            List<PayrollItemParameterOption>>() { });
            if (options.stream().noneMatch(option -> option.value().equals(value))) {
                throw new IllegalArgumentException(
                        definition.getDisplayName() + " の選択値が不正です。"
                );
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException(
                    definition.getDisplayName() + " の選択肢定義を読み込めません。",
                    exception
            );
        }
    }

    private String write(Map<String, String> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters == null ? Map.of() : parameters);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("手当・控除設定を保存できません。", exception);
        }
    }

    private Map<String, String> read(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(
                    json,
                    new com.fasterxml.jackson.core.type.TypeReference<
                            Map<String, String>>() { }
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "手当・控除設定を読み込めません。",
                    exception
            );
        }
    }
}
