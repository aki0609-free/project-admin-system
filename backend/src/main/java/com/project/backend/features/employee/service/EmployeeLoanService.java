package com.project.backend.features.employee.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.dto.EmployeeLoanResponse;
import com.project.backend.features.employee.dto.EmployeeLoanSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeLoan;
import com.project.backend.features.employee.enums.ApprovalStatus;
import com.project.backend.features.employee.mapper.EmployeeLoanMapper;
import com.project.backend.features.employee.repository.EmployeeLoanRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeLoanService {

    private final EmployeeLoanRepository repository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeLoanMapper mapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EmployeeLoanResponse> findAll() {
        return mapper.toResponseList(
                repository.findAllByDeletedAtIsNullOrderByIdDesc()
        );
    }

    @Transactional(readOnly = true)
    public EmployeeLoanResponse findDetail(Long id) {
        EmployeeLoan entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員貸付が見つかりません。 id=" + id));

        return mapper.toResponse(entity);
    }

    @Transactional
    public EmployeeLoanResponse create(EmployeeLoanSaveRequest request) {
        validateRequest(request);

        Employee employee = findEmployee(request.getEmployeeId());
        verifyEmployeeCanRegisterFinance(employee);
        verifySingleActiveLoan(request.getEmployeeId(), null, request.isActiveFlag());

        EmployeeLoan entity = new EmployeeLoan();
        mapper.updateFromRequest(request, entity, employee);
        entity.setCurrentBalance(entity.getPrincipal());
        entity.setApprovalStatus(ApprovalStatus.APPROVED);
        entity.setApprovalComment(null);

        return mapper.toResponse(repository.save(entity));
    }

    @SuppressWarnings("null")
    @Transactional
    public EmployeeLoanResponse update(Long id, EmployeeLoanSaveRequest request) {
        validateRequest(request);

        EmployeeLoan entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員貸付が見つかりません。 id=" + id));

        if (!entity.getEmployee().getId().equals(request.getEmployeeId())) {
            throw new IllegalArgumentException("貸付登録後に従業員は変更できません。");
        }

        verifySingleActiveLoan(request.getEmployeeId(), id, request.isActiveFlag());
        verifyPrincipalChange(entity, request);
        if (request.isActiveFlag()
                && nvl(entity.getCurrentBalance()).compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("完済済みの貸付は再度有効にできません。");
        }

        Employee employee = entity.getEmployee();
        BigDecimal previousPrincipal = entity.getPrincipal();
        BigDecimal previousBalance = entity.getCurrentBalance();
        mapper.updateFromRequest(request, entity, employee);
        if (previousPrincipal.compareTo(request.getPrincipal()) != 0) {
            entity.setCurrentBalance(request.getPrincipal());
        } else {
            entity.setCurrentBalance(previousBalance);
        }
        entity.setApprovalStatus(ApprovalStatus.APPROVED);
        entity.setApprovalComment(null);

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        EmployeeLoan entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員貸付が見つかりません。 id=" + id));

        BigDecimal balance = nvl(entity.getCurrentBalance());
        boolean untouched = balance.compareTo(nvl(entity.getPrincipal())) == 0;
        boolean completed = balance.compareTo(BigDecimal.ZERO) == 0;
        if (!untouched && !completed) {
            throw new IllegalArgumentException(
                    "一部返済済みで残高がある貸付は削除できません。有効を解除してください。"
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
            throw new IllegalArgumentException("退職済みの従業員へ新しい貸付は登録できません。");
        }
    }

    private void verifySingleActiveLoan(Long employeeId, Long currentId, boolean active) {
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
            throw new IllegalArgumentException("この従業員には有効な貸付が既に登録されています。");
        }
    }

    private void verifyPrincipalChange(EmployeeLoan entity, EmployeeLoanSaveRequest request) {
        BigDecimal oldPrincipal = nvl(entity.getPrincipal());
        BigDecimal currentBalance = nvl(entity.getCurrentBalance());
        BigDecimal requestedPrincipal = nvl(request.getPrincipal());

        if (oldPrincipal.compareTo(requestedPrincipal) != 0
                && currentBalance.compareTo(oldPrincipal) != 0) {
            throw new IllegalArgumentException(
                    "返済開始後は借入元本を変更できません。"
            );
        }
    }

    private void validateRequest(EmployeeLoanSaveRequest request) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (request.getEmployeeId() == null) {
            throw new RuntimeException("employeeId は必須です。");
        }

        if (nvl(request.getPrincipal()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("借入元本は0円より大きい金額を指定してください。");
        }

        if (nvl(request.getMonthlyRepayment()).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("月返済額は0円以上で指定してください。");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
