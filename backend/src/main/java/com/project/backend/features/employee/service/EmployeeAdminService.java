package com.project.backend.features.employee.service;

import java.time.Instant;
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
import com.project.backend.features.employee.mapper.EmployeeMapper;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeePayrollProfileRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.employee.repository.EmployeeResignationChecklistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeAdminService {

    private final EmployeeRepository employeeRepository;
    private final EmployeePayrollProfileRepository payrollProfileRepository;
    private final EmployeeContractRepository contractRepository;
    private final EmployeeResignationChecklistRepository resignationChecklistRepository;
    private final EmployeeMapper mapper;

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

        return mapper.toDetailResponse(employee, payrollProfile, contract);
    }

    @Transactional
    public EmployeeDetailResponse create(EmployeeSaveRequest request) {
        validateRequest(request, null);

        Employee employee = new Employee();
        mapper.updateEmployeeFromRequest(request, employee);

        Employee savedEmployee = employeeRepository.save(employee);

        EmployeePayrollProfile profile = new EmployeePayrollProfile();
        profile.setEmployee(savedEmployee);
        mapper.updatePayrollProfileFromRequest(request.payrollProfile(), profile);
        payrollProfileRepository.save(profile);

        EmployeeContract contract = new EmployeeContract();
        contract.setEmployee(savedEmployee);
        mapper.updateContractFromRequest(request.contract(), contract);
        contractRepository.save(contract);

        return mapper.toDetailResponse(savedEmployee, profile, contract);
    }

    @SuppressWarnings("null")
    @Transactional
    public EmployeeDetailResponse update(Long id, EmployeeSaveRequest request) {
        validateRequest(request, id);

        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 id=" + id));

        mapper.updateEmployeeFromRequest(request, employee);

        EmployeePayrollProfile profile = payrollProfileRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .orElseGet(() -> {
                    EmployeePayrollProfile created = new EmployeePayrollProfile();
                    created.setEmployee(employee);
                    return created;
                });

        mapper.updatePayrollProfileFromRequest(request.payrollProfile(), profile);
        payrollProfileRepository.save(profile);

        EmployeeContract contract = contractRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .orElseGet(() -> {
                    EmployeeContract created = new EmployeeContract();
                    created.setEmployee(employee);
                    return created;
                });

        mapper.updateContractFromRequest(request.contract(), contract);
        contractRepository.save(contract);

        Employee savedEmployee = employeeRepository.save(employee);

        return mapper.toDetailResponse(savedEmployee, profile, contract);
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

        employee.setResignDate(request.resignDate());
        employee.setActiveFlag(false);
        employee.setEmploymentStatus(EmploymentStatus.RESIGNED);

        Employee savedEmployee = employeeRepository.save(employee);

        EmployeePayrollProfile payrollProfile = payrollProfileRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .orElse(null);

        EmployeeContract contract = contractRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .orElse(null);

        return mapper.toDetailResponse(savedEmployee, payrollProfile, contract);
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 id=" + id));

        Instant now = Instant.now();
        employee.setDeletedAt(now);

        payrollProfileRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .ifPresent(profile -> profile.setDeletedAt(now));

        contractRepository.findByEmployeeIdAndDeletedAtIsNull(id)
                .ifPresent(contract -> contract.setDeletedAt(now));
    }

    private void validateRequest(EmployeeSaveRequest request, Long id) {
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

        boolean exists = id == null
                ? employeeRepository.existsByEmployeeCodeAndDeletedAtIsNull(request.employeeCode())
                : employeeRepository.existsByEmployeeCodeAndIdNotAndDeletedAtIsNull(
                        request.employeeCode(),
                        id);

        if (exists) {
            throw new RuntimeException("employeeCode が重複しています。 employeeCode=" + request.employeeCode());
        }

        if (request.payrollProfile() != null
                && request.payrollProfile().taxDependentCount() != null
                && request.payrollProfile().taxDependentCount() < 0) {
            throw new RuntimeException("taxDependentCount は0以上で指定してください。");
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