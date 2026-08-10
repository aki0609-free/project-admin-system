package com.project.backend.features.master.payrollitem.balance;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
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
        var policy = policyRepository
                .findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                        TenantContext.getTenantId(), targetType, targetCode)
                .orElse(null);
        if (policy == null) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        var current = enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        employeeId, policy.getId()
                );

        if (enabled && current.isEmpty()) {
            EmployeePayrollItemEnrollment enrollment = new EmployeePayrollItemEnrollment();
            enrollment.setEmployeeId(employeeId);
            enrollment.setBalancePolicyId(policy.getId());
            enrollment.setEffectiveFrom(today);
            enrollment.setSettingsJson(write(parameters));
            enrollmentRepository.save(enrollment);
        } else if (enabled) {
            current.get().setSettingsJson(write(parameters));
        } else if (!enabled && current.isPresent()) {
            // effectiveTo は適用最終日（包含）として扱う。
            // 無効化操作後に作成する当日の日報へ残さないため、前日で終了する。
            current.get().setEffectiveTo(today.minusDays(1));
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
