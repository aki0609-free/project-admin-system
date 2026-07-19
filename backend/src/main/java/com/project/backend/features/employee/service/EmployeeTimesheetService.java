package com.project.backend.features.employee.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.dto.EmployeeTimesheetResponse;
import com.project.backend.features.employee.dto.EmployeeTimesheetSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeTimesheet;
import com.project.backend.features.employee.mapper.EmployeeTimesheetMapper;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.employee.repository.EmployeeTimesheetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeTimesheetService {

    private final EmployeeTimesheetRepository repository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeTimesheetMapper mapper;

    @Transactional(readOnly = true)
    public List<EmployeeTimesheetResponse> findAll() {
        return mapper.toResponseList(
                repository.findAllByDeletedAtIsNullOrderByWorkDateDescIdDesc()
        );
    }

    @Transactional(readOnly = true)
    public EmployeeTimesheetResponse findDetail(Long id) {
        EmployeeTimesheet entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("勤怠データが見つかりません。 id=" + id));

        return mapper.toResponse(entity);
    }

    @Transactional
    public EmployeeTimesheetResponse create(EmployeeTimesheetSaveRequest request) {
        validateRequest(request);

        Employee employee = findEmployee(request.getEmployeeId());

        EmployeeTimesheet entity = new EmployeeTimesheet();
        mapper.updateFromRequest(request, entity, employee);

        return mapper.toResponse(repository.save(entity));
    }

    @SuppressWarnings("null")
    @Transactional
    public EmployeeTimesheetResponse update(Long id, EmployeeTimesheetSaveRequest request) {
        validateRequest(request);

        EmployeeTimesheet entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("勤怠データが見つかりません。 id=" + id));

        Employee employee = findEmployee(request.getEmployeeId());
        mapper.updateFromRequest(request, entity, employee);

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        EmployeeTimesheet entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("勤怠データが見つかりません。 id=" + id));

        entity.setDeletedAt(Instant.now());
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepository.findByIdAndDeletedAtIsNull(employeeId)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません。 employeeId=" + employeeId));
    }

    private void validateRequest(EmployeeTimesheetSaveRequest request) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (request.getEmployeeId() == null) {
            throw new RuntimeException("employeeId は必須です。");
        }

        if (request.getWorkDate() == null) {
            throw new RuntimeException("workDate は必須です。");
        }
    }
}