package com.project.backend.features.master.payrollitem.balance;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.employee.dto.EmployeePayrollItemSettingRequest;
import com.project.backend.features.employee.dto.EmployeePayrollItemSettingResponse;
import com.project.backend.features.employee.dto.PayrollItemParameterDefinitionResponse;
import com.project.backend.features.employee.dto.PayrollItemParameterOptionResponse;
import com.project.backend.features.master.allowance.repository.AllowanceMasterRepository;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemInputSource;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.parameter.PayrollItemRuleParameterResolutionContext;
import com.project.backend.features.master.payrollitem.parameter.PayrollItemRuleParameterResolverRegistry;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeePayrollItemSettingService {
    private final PayrollItemBalancePolicyRepository policyRepository;
    private final EmployeePayrollItemEnrollmentRepository enrollmentRepository;
    private final PayrollItemBalanceQueryService balanceQueryService;
    private final PayrollItemEnrollmentService enrollmentService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final DeductionMasterRepository deductionMasterRepository;
    private final AllowanceMasterRepository allowanceMasterRepository;
    private final PayrollItemParameterDefinitionRepository parameterDefinitionRepository;
    private final PayrollItemRuleParameterResolverRegistry parameterResolverRegistry;

    @Transactional(readOnly = true)
    public List<EmployeePayrollItemSettingResponse> findAll(Long employeeId) {
        var enrollments = enrollmentRepository
                .findAllByEmployeeIdAndDeletedAtIsNullOrderByEffectiveFromAsc(employeeId);
        return policyRepository
                .findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc(
                        TenantContext.getTenantId())
                .stream()
                .filter(PayrollItemBalancePolicy::isActiveFlag)
                .filter(policy -> policy.getApplicationScope()
                        == PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT)
                .filter(policy -> findEnabledMasterName(policy) != null)
                .map(policy -> {
            var current = enrollments.stream()
                    .filter(e -> e.getBalancePolicyId().equals(policy.getId()) && e.getEffectiveTo() == null)
                    .reduce((first, second) -> second).orElse(null);
            var last = current != null ? current : enrollments.stream()
                    .filter(e -> e.getBalancePolicyId().equals(policy.getId()))
                    .reduce((first, second) -> second).orElse(null);
            var balance = policy.getTargetType() == PayrollItemTargetType.DEDUCTION
                    ? balanceQueryService.findDeductionBalance(
                            employeeId, policy.getTargetMasterId(), LocalDate.now(clock), null)
                    : balanceQueryService.findAllowanceBalance(
                            employeeId, policy.getTargetMasterId(), LocalDate.now(clock), null);
            return new EmployeePayrollItemSettingResponse(
                    policy.getTargetType().name(),
                    policy.getTargetCode(), policy.getDisplayName(), current != null,
                    last == null ? null : last.getEffectiveFrom(), last == null ? null : last.getEffectiveTo(),
                    policy.getInputSource().name(), policy.isBalanceTrackingFlag(),
                    policy.getBalanceUnit().name(), balance.openingQuantity(), balance.accruedQuantity(),
                    balance.consumedQuantity(), balance.remainingQuantity(),
                    withDefaults(policy, read(last == null ? null : last.getSettingsJson())),
                    parameterDefinitions(policy));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeePayrollItemSettingResponse> findCatalog() {
        return policyRepository
                .findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc(
                        TenantContext.getTenantId())
                .stream()
                .filter(PayrollItemBalancePolicy::isActiveFlag)
                .filter(policy -> policy.getApplicationScope()
                        == PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT)
                .map(policy -> {
                    String masterName = findEnabledMasterName(policy);
                    return masterName == null ? null : new EmployeePayrollItemSettingResponse(
                            policy.getTargetType().name(), policy.getTargetCode(), masterName, false,
                            null, null, policy.getInputSource().name(),
                            policy.isBalanceTrackingFlag(), policy.getBalanceUnit().name(),
                            java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                            java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                            withDefaults(policy, Map.of()), parameterDefinitions(policy));
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isDailyReportInputEnabled(
            Long employeeId,
            Long deductionMasterId,
            LocalDate targetDate
    ) {
        var policy = policyRepository
                .findByTenantIdAndTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        TenantContext.getTenantId(),
                        PayrollItemTargetType.DEDUCTION,
                        deductionMasterId
                )
                .orElse(null);
        if (policy == null) {
            return true;
        }
        if (policy.getApplicationScope() == PayrollItemApplicationScope.ALL_EMPLOYEES) {
            return policy.getInputSource() == PayrollItemInputSource.DAILY_REPORT;
        }
        if (policy.getInputSource() == PayrollItemInputSource.TRANSACTION) {
            return false;
        }
        return enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        employeeId, policy.getId(), targetDate
                )
                .stream()
                .filter(enrollment -> enrollment.getEffectiveTo() == null
                        || !enrollment.getEffectiveTo().isBefore(targetDate))
                .reduce((first, second) -> second)
                .map(enrollment -> effectiveInputSource(
                        policy, read(enrollment.getSettingsJson()))
                        == PayrollItemInputSource.DAILY_REPORT)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Map<Long, Map<String, Object>> findDailyRuleParameters(
            Long employeeId,
            PayrollItemTargetType targetType,
            LocalDate targetDate
    ) {
        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        policyRepository
                .findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc(
                        TenantContext.getTenantId())
                .stream()
                .filter(PayrollItemBalancePolicy::isActiveFlag)
                .filter(policy -> policy.getTargetType() == targetType)
                .forEach(policy -> {
                    Map<String, String> values;
                    if (policy.getApplicationScope()
                            == PayrollItemApplicationScope.ALL_EMPLOYEES) {
                        values = withDefaults(policy, Map.of());
                    } else {
                        var enrollment = enrollmentRepository
                                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                                        employeeId, policy.getId(), targetDate)
                                .stream()
                                .filter(item -> item.getEffectiveTo() == null
                                        || !item.getEffectiveTo().isBefore(targetDate))
                                .reduce((first, second) -> second)
                                .orElse(null);
                        if (enrollment == null) return;
                        values = withDefaults(
                                policy, read(enrollment.getSettingsJson()));
                    }
                    if (effectiveInputSource(policy, values)
                            != PayrollItemInputSource.DAILY_REPORT) return;

                    Map<String, Object> parameters = new LinkedHashMap<>();
                    parameterDefinitionRepository
                            .findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                                    TenantContext.getTenantId(), policy.getId())
                            .stream()
                            .filter(PayrollItemParameterDefinition::isRuleParameterFlag)
                            .forEach(definition -> {
                                if (definition.getRuleValueResolverKey() != null
                                        && !definition.getRuleValueResolverKey().isBlank()) {
                                    parameters.put(
                                            definition.getParameterKey(),
                                            parameterResolverRegistry.resolve(
                                                    definition.getRuleValueResolverKey(),
                                                    new PayrollItemRuleParameterResolutionContext(
                                                            employeeId,
                                                            targetDate,
                                                            values,
                                                            policy.getId(),
                                                            null
                                                    )
                                            )
                                    );
                                    return;
                                }
                                String value = values.get(definition.getParameterKey());
                                if (value != null && !value.isBlank()) {
                                    parameters.put(
                                            definition.getParameterKey(),
                                            convertParameter(definition, value)
                                    );
                                }
                            });
                    result.put(policy.getTargetMasterId(), parameters);
                });
        return result;
    }

    @Transactional(readOnly = true)
    public Set<Long> findExcludedDailyMasterIds(
            Long employeeId,
            PayrollItemTargetType targetType,
            LocalDate targetDate
    ) {
        return policyRepository
                .findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc(
                        TenantContext.getTenantId())
                .stream()
                .filter(PayrollItemBalancePolicy::isActiveFlag)
                .filter(policy -> policy.getTargetType() == targetType)
                .filter(policy -> !isPolicyDailyEnabled(
                        policy, employeeId, targetDate))
                .map(PayrollItemBalancePolicy::getTargetMasterId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean isPolicyDailyEnabled(
            PayrollItemBalancePolicy policy,
            Long employeeId,
            LocalDate targetDate
    ) {
        if (policy.getApplicationScope() == PayrollItemApplicationScope.ALL_EMPLOYEES) {
            return policy.getInputSource() == PayrollItemInputSource.DAILY_REPORT;
        }
        return enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        employeeId, policy.getId(), targetDate)
                .stream()
                .filter(item -> item.getEffectiveTo() == null
                        || !item.getEffectiveTo().isBefore(targetDate))
                .reduce((first, second) -> second)
                .map(item -> effectiveInputSource(
                        policy, withDefaults(policy, read(item.getSettingsJson())))
                        == PayrollItemInputSource.DAILY_REPORT)
                .orElse(false);
    }

    @Transactional
    public void synchronizeAll(Long employeeId, List<EmployeePayrollItemSettingRequest> settings) {
        synchronizeAll(employeeId, settings, LocalDate.now(clock));
    }

    @Transactional
    public void synchronizeAll(
            Long employeeId,
            List<EmployeePayrollItemSettingRequest> settings,
            LocalDate effectiveFrom
    ) {
        if (settings == null) return;
        settings.forEach(setting -> enrollmentService.synchronize(
                employeeId, setting.targetType(),
                setting.targetCode(), setting.enabled(), setting.parameters(), effectiveFrom));
    }

    private String findEnabledMasterName(PayrollItemBalancePolicy policy) {
        String tenantId = TenantContext.getTenantId();
        if (policy.getTargetType() == PayrollItemTargetType.ALLOWANCE) {
            return allowanceMasterRepository
                    .findByTenantIdAndAllowanceCodeAndDeletedAtIsNull(
                            tenantId, policy.getTargetCode())
                    .filter(master -> Boolean.TRUE.equals(master.getEnabled()))
                    .map(master -> master.getAllowanceName())
                    .orElse(null);
        }
        return deductionMasterRepository
                .findByTenantIdAndDeductionCodeAndDeletedAtIsNull(
                        tenantId, policy.getTargetCode())
                .filter(master -> Boolean.TRUE.equals(master.getEnabled()))
                .map(master -> master.getDeductionName())
                .orElse(null);
    }

    private List<PayrollItemParameterDefinitionResponse> parameterDefinitions(
            PayrollItemBalancePolicy policy
    ) {
        return parameterDefinitionRepository
                .findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        TenantContext.getTenantId(), policy.getId())
                .stream()
                .map(definition -> new PayrollItemParameterDefinitionResponse(
                        definition.getParameterKey(), definition.getDisplayName(),
                        definition.getInputType().name(), definition.isRequiredFlag(),
                        definition.getDefaultValue(), readOptions(definition.getOptionsJson()),
                        definition.isRuleParameterFlag(), definition.isDailyDisplayFlag(),
                        definition.isInputSourceOverrideFlag(),
                        definition.getRuleValueResolverKey(),
                        definition.getDisplayOrder()))
                .toList();
    }

    private List<PayrollItemParameterOptionResponse> readOptions(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<PayrollItemParameterOptionResponse>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("給与項目の選択肢定義を読み込めません。", exception);
        }
    }

    private Map<String, String> withDefaults(
            PayrollItemBalancePolicy policy,
            Map<String, String> values
    ) {
        Map<String, String> result = new LinkedHashMap<>(values);
        parameterDefinitionRepository
                .findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        TenantContext.getTenantId(), policy.getId())
                .forEach(definition -> {
                    if (definition.getDefaultValue() != null) {
                        result.putIfAbsent(definition.getParameterKey(), definition.getDefaultValue());
                    }
                });
        return result;
    }

    private PayrollItemInputSource effectiveInputSource(
            PayrollItemBalancePolicy policy,
            Map<String, String> parameters
    ) {
        return parameterDefinitionRepository
                .findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        TenantContext.getTenantId(), policy.getId())
                .stream()
                .filter(PayrollItemParameterDefinition::isInputSourceOverrideFlag)
                .findFirst()
                .map(definition -> parameters.get(definition.getParameterKey()))
                .filter(value -> value != null && !value.isBlank())
                .map(value -> {
                    try {
                        return PayrollItemInputSource.valueOf(value);
                    } catch (IllegalArgumentException exception) {
                        throw new IllegalStateException(
                                "入力元パラメーターが不正です。targetCode="
                                        + policy.getTargetCode(), exception
                        );
                    }
                })
                .orElse(policy.getInputSource());
    }

    private Object convertParameter(
            PayrollItemParameterDefinition definition,
            String value
    ) {
        return switch (definition.getInputType()) {
            case NUMBER -> new java.math.BigDecimal(value);
            case BOOLEAN -> Boolean.valueOf(value);
            case DATE -> LocalDate.parse(value);
            case SELECT, TEXT -> value;
        };
    }

    private Map<String, String> read(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("手当・控除設定を読み込めません。", exception);
        }
    }

}
