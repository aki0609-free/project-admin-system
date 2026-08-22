package com.project.backend.features.employee.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.features.employee.dto.EmployeeDetailResponse;
import com.project.backend.features.employee.dto.EmployeeListItemResponse;
import com.project.backend.features.employee.dto.EmployeeResignRequest;
import com.project.backend.features.employee.dto.EmployeeSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.entity.EmployeePayrollProfile;
import com.project.backend.features.employee.entity.EmployeeResignationChecklistMaster;
import com.project.backend.features.employee.enums.EmploymentStatus;
import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.mapper.EmployeeMapper;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeePayrollProfileRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.employee.repository.EmployeeResignationChecklistRepository;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalanceSnapshot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeAdminService {

    private final EmployeeRepository employeeRepository;
    private final EmployeePayrollProfileRepository payrollProfileRepository;
    private final EmployeeContractRepository contractRepository;
    private final EmployeeResignationChecklistRepository resignationChecklistRepository;
    private final EmployeeMapper mapper;
    private final EmployeeDeletionPolicy deletionPolicy;
    private final com.project.backend.features.master.payrollitem.balance.EmployeePayrollItemSettingService payrollItemSettingService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EmployeeListItemResponse> findAll() {
        return mapper.toListItemResponseList(
                employeeRepository.findAllByDeletedAtIsNullOrderByIdAsc());
    }

    @Transactional(readOnly = true)
    public EmployeeDetailResponse findDetail(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 id=" + id));

        EmployeePayrollProfile payrollProfile = payrollProfileRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .orElse(null);

        EmployeeContract contract = contractRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .orElse(null);

        return toDetailResponse(employee, payrollProfile, contract);
    }

    @Transactional
    public EmployeeDetailResponse create(EmployeeSaveRequest request) {
        validateRequest(request, null, null);

        Employee employee = new Employee();
        employee.setEmployeeCode(request.employeeCode().trim());
        mapper.updateEmployeeFromRequest(request, employee);
        employee.setEmployeeName(request.employeeName().trim());
        employee.initializeEmployment();

        Employee savedEmployee = employeeRepository.save(employee);
        LocalDate initialSettingDate = request.hireDate() == null
                ? LocalDate.now(clock)
                : request.hireDate();
        payrollItemSettingService.synchronizeAll(
                savedEmployee.getId(),
                request.payrollItemSettings(),
                initialSettingDate
        );

        EmployeePayrollProfile profile = new EmployeePayrollProfile();
        profile.setEmployee(savedEmployee);
        mapper.updatePayrollProfileFromRequest(request.payrollProfile(), profile);

        EmployeeContract contract = new EmployeeContract();
        contract.setEmployee(savedEmployee);
        mapper.updateContractFromRequest(request.contract(), contract);
        synchronizeLegacyDailyPayFlag(profile, contract);
        payrollProfileRepository.save(profile);
        contractRepository.save(contract);

        return toDetailResponse(savedEmployee, profile, contract);
    }

    @SuppressWarnings("null")
    @Transactional
    public EmployeeDetailResponse update(Long id, EmployeeSaveRequest request) {
        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 id=" + id));

        validateRequest(request, id, employee);

        mapper.updateEmployeeFromRequest(request, employee);
        employee.setEmployeeName(request.employeeName().trim());
        employee.changeEmploymentStatus(request.employmentStatus());

        EmployeePayrollProfile profile = payrollProfileRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .orElseGet(() -> {
                    EmployeePayrollProfile created = new EmployeePayrollProfile();
                    created.setEmployee(employee);
                    return created;
        });

        mapper.updatePayrollProfileFromRequest(request.payrollProfile(), profile);

        EmployeeContract contract = contractRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .orElseGet(() -> {
                    EmployeeContract created = new EmployeeContract();
                    created.setEmployee(employee);
                    return created;
                });

        mapper.updateContractFromRequest(request.contract(), contract);
        synchronizeLegacyDailyPayFlag(profile, contract);
        payrollProfileRepository.save(profile);
        contractRepository.save(contract);

        Employee savedEmployee = employeeRepository.save(employee);

        payrollItemSettingService.synchronizeAll(savedEmployee.getId(), request.payrollItemSettings());

        return toDetailResponse(savedEmployee, profile, contract);
    }

    @Transactional
    public EmployeeDetailResponse resign(
            Long id,
            EmployeeResignRequest request
    ) {
        validateResignRequest(request);

        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 id=" + id));

        validateRequiredChecklist(request);

        employee.resign(request.resignDate());

        Employee savedEmployee = employeeRepository.save(employee);

        EmployeePayrollProfile payrollProfile = payrollProfileRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .orElse(null);

        EmployeeContract contract = contractRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .orElse(null);

        return toDetailResponse(savedEmployee, payrollProfile, contract);
    }

    @Transactional
    public EmployeeDetailResponse cancelResignation(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 id=" + id));

        employee.cancelResignation(EmploymentStatus.ACTIVE);
        Employee savedEmployee = employeeRepository.save(employee);

        EmployeePayrollProfile payrollProfile = payrollProfileRepository
                .findByEmployeeIdAndDeletedAtIsNull(id)
                .orElse(null);
        EmployeeContract contract = contractRepository
                .findByEmployeeIdAndDeletedAtIsNull(id)
                .orElse(null);

        return toDetailResponse(savedEmployee, payrollProfile, contract);
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 id=" + id));

        deletionPolicy.verifyDeletable(id);

        Instant now = Instant.now(clock);
        employee.setDeletedAt(now);

        payrollProfileRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .ifPresent(profile -> profile.setDeletedAt(now));

        contractRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .ifPresent(contract -> contract.setDeletedAt(now));
    }

    private EmployeeDetailResponse toDetailResponse(
            Employee employee,
            EmployeePayrollProfile payrollProfile,
            EmployeeContract contract
    ) {
        EmployeePayrollProfile resolvedPayrollProfile = payrollProfile;
        if (resolvedPayrollProfile == null) {
            resolvedPayrollProfile = new EmployeePayrollProfile();
            resolvedPayrollProfile.setEmployee(employee);
        }

        EmployeeContract resolvedContract = contract;
        if (resolvedContract == null) {
            resolvedContract = new EmployeeContract();
            resolvedContract.setEmployee(employee);
        }

        return mapper.toDetailResponse(
                employee, resolvedPayrollProfile, resolvedContract,
                PayrollItemBalanceSnapshot.untracked(),
                payrollItemSettingService.findAll(employee.getId())
        );
    }

    /**
     * 既存DB列を残したまま、日払い判定の入力元を支払サイクルへ一本化する。
     */
    @SuppressWarnings("deprecation")
    private void synchronizeLegacyDailyPayFlag(
            EmployeePayrollProfile profile,
            EmployeeContract contract
    ) {
        profile.setDailyPayFlag(contract.getPaymentCycle() == PaymentCycle.DAILY);
    }

    private void validateRequest(
            EmployeeSaveRequest request,
            Long id,
            Employee current
    ) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.employeeCode())) {
            throw new RuntimeException("employeeCode は必須です。");
        }

        if (!StringUtils.hasText(request.employeeName())) {
            throw new RuntimeException("employeeName は必須です。");
        }

        if (request.employmentType() == null) {
            throw new RuntimeException("employmentType は必須です。");
        }

        if (request.employmentStatus() == null) {
            throw new RuntimeException("employmentStatus は必須です。");
        }

        if (id == null) {
            if (request.employmentStatus() != EmploymentStatus.ACTIVE
                    || request.resignDate() != null
                    || Boolean.FALSE.equals(request.activeFlag())) {
                throw new IllegalArgumentException(
                        "新規従業員は在籍状態で登録してください。"
                );
            }
        } else {
            if (current == null) {
                throw new IllegalArgumentException("現在の従業員情報は必須です。");
            }
            if (!current.getEmployeeCode().equals(request.employeeCode().trim())) {
                throw new IllegalArgumentException(
                        "社員コードは作成後に変更できません。"
                );
            }
            if (current.getEmploymentStatus() == EmploymentStatus.RESIGNED) {
                throw new IllegalStateException(
                        "退職済み従業員は通常編集できません。退職取消後に編集してください。"
                );
            }
            if (request.employmentStatus() == EmploymentStatus.RESIGNED
                    || request.resignDate() != null
                    || Boolean.FALSE.equals(request.activeFlag())) {
                throw new IllegalArgumentException(
                        "退職への変更は退職処理から実行してください。"
                );
            }
        }

        boolean exists = id == null
                ? employeeRepository.existsByEmployeeCodeAndDeletedAtIsNull(
                        request.employeeCode().trim()
                )
                : employeeRepository.existsByEmployeeCodeAndIdNotAndDeletedAtIsNull(
                        request.employeeCode().trim(),
                        id);

        if (exists) {
            throw new RuntimeException("employeeCode が重複しています。 employeeCode=" + request.employeeCode());
        }

        if (request.payrollProfile() != null
                && request.payrollProfile().taxDependentCount() != null
                && request.payrollProfile().taxDependentCount() < 0) {
            throw new RuntimeException("taxDependentCount は0以上で指定してください。");
        }

        if (request.contract() != null
                && request.contract().contractStartDate() != null
                && request.contract().contractEndDate() != null
                && request.contract().contractEndDate().isBefore(
                        request.contract().contractStartDate()
                )) {
            throw new IllegalArgumentException(
                    "契約終了日は契約開始日以降で指定してください。"
            );
        }

    }

    private void validateResignRequest(EmployeeResignRequest request) {
        if (request == null) {
            throw new RuntimeException("退職処理リクエストは必須です。");
        }

        if (request.resignDate() == null) {
            throw new RuntimeException("退職日は必須です。");
        }
    }

    @SuppressWarnings("null")
    private void validateRequiredChecklist(EmployeeResignRequest request) {
        List<Long> checkedIds = request.checkedChecklistIds() != null
                ? request.checkedChecklistIds()
                : List.of();

        List<EmployeeResignationChecklistMaster> requiredItems =
                resignationChecklistRepository
                        .findAllByActiveFlagTrueAndRequiredFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc();

        List<String> missingNames = requiredItems.stream()
                .filter(item -> !checkedIds.contains(item.getId()))
                .map(EmployeeResignationChecklistMaster::getName)
                .toList();

        if (!missingNames.isEmpty()) {
            throw new RuntimeException("必須チェックが未完了です。 " + String.join(", ", missingNames));
        }
    }
}
