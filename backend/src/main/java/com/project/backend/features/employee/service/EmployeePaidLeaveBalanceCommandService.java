package com.project.backend.features.employee.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.entity.EmployeePayrollProfile;
import com.project.backend.features.employee.repository.EmployeePayrollProfileRepository;

import lombok.RequiredArgsConstructor;

/** 日報の有給使用差分を従業員の有給残へ反映する。 */
@Service
@RequiredArgsConstructor
public class EmployeePaidLeaveBalanceCommandService {

    private final EmployeePayrollProfileRepository repository;

    @Transactional
    public void applyUsageDiff(Long employeeId, BigDecimal usageDiff) {
        BigDecimal diff = nvl(usageDiff);
        if (diff.signum() == 0) {
            return;
        }

        EmployeePayrollProfile profile = repository
                .findForUpdateByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "従業員の給与・税金情報が見つからないため、有給残を更新できません。"
                ));

        BigDecimal current = nvl(profile.getPaidLeaveRemainingDays());
        BigDecimal updated = current.subtract(diff);
        if (updated.signum() < 0) {
            throw new IllegalArgumentException(
                    "有給取得日数が有給残日数を超えています。残日数=" + current
            );
        }
        profile.setPaidLeaveRemainingDays(updated);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
