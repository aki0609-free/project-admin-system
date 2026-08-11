package com.project.backend.features.master.payrollitem.balance;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.employee.dto.EmployeePayrollItemSettingRequest;
import com.project.backend.features.employee.dto.EmployeePayrollItemSettingResponse;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemInputSource;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeePayrollItemSettingService {
    private static final String COLLECTION_MODE = "collectionMode";
    private static final String MONTHLY = "MONTHLY";
    private final PayrollItemBalancePolicyRepository policyRepository;
    private final EmployeePayrollItemEnrollmentRepository enrollmentRepository;
    private final PayrollItemBalanceQueryService balanceQueryService;
    private final PayrollItemEnrollmentService enrollmentService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final DeductionMasterRepository deductionMasterRepository;

    @Transactional(readOnly = true)
    public List<EmployeePayrollItemSettingResponse> findAll(Long employeeId) {
        var enrollments = enrollmentRepository
                .findAllByEmployeeIdAndDeletedAtIsNullOrderByEffectiveFromAsc(employeeId);
        return policyRepository
                .findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc(
                        TenantContext.getTenantId())
                .stream()
                .filter(policy -> deductionMasterRepository
                        .findByTenantIdAndDeductionCodeAndDeletedAtIsNull(
                                TenantContext.getTenantId(), policy.getTargetCode())
                        .filter(master -> Boolean.TRUE.equals(master.getEnabled()))
                        .isPresent())
                .map(policy -> {
            var current = enrollments.stream()
                    .filter(e -> e.getBalancePolicyId().equals(policy.getId()) && e.getEffectiveTo() == null)
                    .reduce((first, second) -> second).orElse(null);
            var last = current != null ? current : enrollments.stream()
                    .filter(e -> e.getBalancePolicyId().equals(policy.getId()))
                    .reduce((first, second) -> second).orElse(null);
            var balance = balanceQueryService.findDeductionBalance(
                    employeeId, policy.getTargetMasterId(), LocalDate.now(clock), null);
            return new EmployeePayrollItemSettingResponse(
                    policy.getTargetCode(), policy.getDisplayName(), current != null,
                    last == null ? null : last.getEffectiveFrom(), last == null ? null : last.getEffectiveTo(),
                    policy.getInputSource().name(), policy.isBalanceTrackingFlag(),
                    policy.getBalanceUnit().name(), balance.openingQuantity(), balance.accruedQuantity(),
                    balance.consumedQuantity(), balance.remainingQuantity(),
                    read(last == null ? null : last.getSettingsJson()));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeePayrollItemSettingResponse> findCatalog() {
        return policyRepository
                .findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc(
                        TenantContext.getTenantId())
                .stream()
                .map(policy -> deductionMasterRepository
                        .findByTenantIdAndDeductionCodeAndDeletedAtIsNull(
                                TenantContext.getTenantId(), policy.getTargetCode())
                        .filter(master -> Boolean.TRUE.equals(master.getEnabled()))
                        .map(master -> new EmployeePayrollItemSettingResponse(
                                policy.getTargetCode(), master.getDeductionName(), false,
                                null, null, policy.getInputSource().name(),
                                policy.isBalanceTrackingFlag(), policy.getBalanceUnit().name(),
                                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                                Map.of()))
                        .orElse(null))
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
                .map(enrollment -> !MONTHLY.equalsIgnoreCase(
                        read(enrollment.getSettingsJson()).get(COLLECTION_MODE)
                ))
                .orElse(false);
    }

    @Transactional
    public void synchronizeAll(Long employeeId, List<EmployeePayrollItemSettingRequest> settings) {
        if (settings == null) return;
        settings.forEach(setting -> enrollmentService.synchronize(
                employeeId, com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType.DEDUCTION,
                setting.targetCode(), setting.enabled(), setting.parameters()));
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
