package com.project.backend.features.employee.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.dto.EmployeeSavingResponse;
import com.project.backend.features.employee.dto.EmployeeSavingSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeSaving;
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

        EmployeeSaving entity = new EmployeeSaving();
        mapper.updateFromRequest(request, entity, employee);

        return mapper.toResponse(repository.save(entity));
    }

    @SuppressWarnings("null")
    @Transactional
    public EmployeeSavingResponse update(Long id, EmployeeSavingSaveRequest request) {
        validateRequest(request);

        EmployeeSaving entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員貯蓄が見つかりません。 id=" + id));

        Employee employee = findEmployee(request.getEmployeeId());
        mapper.updateFromRequest(request, entity, employee);

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        EmployeeSaving entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("従業員貯蓄が見つかりません。 id=" + id));

        entity.setDeletedAt(Instant.now());
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepository.findByIdAndDeletedAtIsNull(employeeId)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 employeeId=" + employeeId));
    }

    private void validateRequest(EmployeeSavingSaveRequest request) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (request.getEmployeeId() == null) {
            throw new RuntimeException("employeeId は必須です。");
        }
    }
}