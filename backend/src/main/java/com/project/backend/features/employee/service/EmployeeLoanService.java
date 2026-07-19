package com.project.backend.features.employee.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.dto.EmployeeLoanResponse;
import com.project.backend.features.employee.dto.EmployeeLoanSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeLoan;
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

        EmployeeLoan entity = new EmployeeLoan();
        mapper.updateFromRequest(request, entity, employee);

        return mapper.toResponse(repository.save(entity));
    }

    @SuppressWarnings("null")
    @Transactional
    public EmployeeLoanResponse update(Long id, EmployeeLoanSaveRequest request) {
        validateRequest(request);

        EmployeeLoan entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員貸付が見つかりません。 id=" + id));

        Employee employee = findEmployee(request.getEmployeeId());
        mapper.updateFromRequest(request, entity, employee);

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        EmployeeLoan entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員貸付が見つかりません。 id=" + id));

        entity.setDeletedAt(Instant.now());
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepository.findByIdAndDeletedAtIsNull(employeeId)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 employeeId=" + employeeId));
    }

    private void validateRequest(EmployeeLoanSaveRequest request) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (request.getEmployeeId() == null) {
            throw new RuntimeException("employeeId は必須です。");
        }
    }
}