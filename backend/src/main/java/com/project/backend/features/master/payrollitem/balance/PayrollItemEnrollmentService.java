package com.project.backend.features.master.payrollitem.balance;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayrollItemEnrollmentService {

    private final PayrollItemBalancePolicyRepository policyRepository;
    private final EmployeePayrollItemEnrollmentRepository enrollmentRepository;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final PayrollItemParameterDefinitionRepository parameterDefinitionRepository;

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

        Map<String, String> validatedParameters = enabled
                ? validateParameters(policy, parameters)
                : Map.of();

        LocalDate today = LocalDate.now(clock);
        LocalDate enrollmentStart = effectiveFrom == null ? today : effectiveFrom;
        var current = enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        employeeId, policy.getId()
                );

        if (enabled && current.isEmpty()) {
            EmployeePayrollItemEnrollment enrollment = new EmployeePayrollItemEnrollment();
            enrollment.setEmployeeId(employeeId);
            enrollment.setBalancePolicyId(policy.getId());
            enrollment.setEffectiveFrom(enrollmentStart);
            enrollment.setSettingsJson(write(validatedParameters));
            enrollmentRepository.save(enrollment);
        } else if (enabled) {
            current.get().setSettingsJson(write(validatedParameters));
        } else if (!enabled && current.isPresent()) {
            // effectiveTo は適用最終日（包含）として扱う。
            // 無効化操作後に作成する当日の日報へ残さないため、前日で終了する。
            current.get().setEffectiveTo(today.minusDays(1));
        }
    }

    private Map<String, String> validateParameters(
            PayrollItemBalancePolicy policy,
            Map<String, String> parameters
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
        return values;
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
}
