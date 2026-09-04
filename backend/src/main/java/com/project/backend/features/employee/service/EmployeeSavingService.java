package com.project.backend.features.employee.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.dto.EmployeeSavingResponse;
import com.project.backend.features.employee.dto.EmployeeSavingSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeSaving;
import com.project.backend.features.employee.enums.ApprovalStatus;
import com.project.backend.features.employee.mapper.EmployeeSavingMapper;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.employee.repository.EmployeeSavingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeSavingService {

    private final EmployeeSavingRepository repository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSavingMapper mapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EmployeeSavingResponse> findAll() {
        return mapper.toResponseList(
                repository.findAllByDeletedAtIsNullOrderByIdDesc()
        );
    }

    @Transactional(readOnly = true)
    public EmployeeSavingResponse findDetail(Long id) {
        EmployeeSaving entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員貯蓄が見つかりません。 id=" + id));

        return mapper.toResponse(entity);
    }

    @Transactional
    public EmployeeSavingResponse create(EmployeeSavingSaveRequest request) {
        validateRequest(request);

        Employee employee = findEmployee(request.getEmployeeId());
        verifyEmployeeCanRegisterFinance(employee);
        verifySingleActiveSaving(request.getEmployeeId(), null, request.isActiveFlag());

        EmployeeSaving entity = new EmployeeSaving();
        mapper.updateFromRequest(request, entity, employee);
        entity.setCurrentBalance(BigDecimal.ZERO);
        entity.setApprovalStatus(ApprovalStatus.APPROVED);
        entity.setApprovalComment(null);

        return mapper.toResponse(repository.save(entity));
    }

    @SuppressWarnings("null")
    @Transactional
    public EmployeeSavingResponse update(Long id, EmployeeSavingSaveRequest request) {
        validateRequest(request);

        EmployeeSaving entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員貯蓄が見つかりません。 id=" + id));

        if (!entity.getEmployee().getId().equals(request.getEmployeeId())) {
            throw new IllegalArgumentException("積立登録後に従業員は変更できません。");
        }

        verifySingleActiveSaving(request.getEmployeeId(), id, request.isActiveFlag());

        Employee employee = entity.getEmployee();
        BigDecimal currentBalance = entity.getCurrentBalance();
        mapper.updateFromRequest(request, entity, employee);
        entity.setCurrentBalance(currentBalance);
        entity.setApprovalStatus(ApprovalStatus.APPROVED);
        entity.setApprovalComment(null);

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        EmployeeSaving entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員貯蓄が見つかりません。 id=" + id));

        if (nvl(entity.getCurrentBalance()).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException(
                    "積立残高があるデータは削除できません。有効を解除してください。"
            );
        }

        entity.setDeletedAt(Instant.now(clock));
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepository.findByIdAndDeletedAtIsNull(employeeId)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 employeeId=" + employeeId));
    }

    private void verifyEmployeeCanRegisterFinance(Employee employee) {
        if (!employee.isActiveFlag()) {
            throw new IllegalArgumentException("退職済みの従業員へ新しい積立設定は登録できません。");
        }
    }

    private void verifySingleActiveSaving(Long employeeId, Long currentId, boolean active) {
        if (!active) {
            return;
        }

        boolean exists = currentId == null
                ? repository.existsByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNull(employeeId)
                : repository.existsByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNullAndIdNot(
                        employeeId,
                        currentId
                );
        if (exists) {
            throw new IllegalArgumentException("この従業員には有効な積立設定が既に登録されています。");
        }
    }

    private void validateRequest(EmployeeSavingSaveRequest request) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (request.getEmployeeId() == null) {
            throw new RuntimeException("employeeId は必須です。");
        }

        BigDecimal percentage = nvl(request.getPercentage());
        if (percentage.compareTo(BigDecimal.ZERO) < 0
                || percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("積立率は0%以上100%以下で指定してください。");
        }

        if (nvl(request.getSavingCalculationBaseAmount()).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("積立計算基礎額は0円以上で指定してください。");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
