package com.project.backend.features.admin.business.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.dto.YearEndAdjustmentResponse;
import com.project.backend.features.admin.business.dto.YearEndAdjustmentSaveRequest;
import com.project.backend.features.admin.business.entity.EmployeeYearEndAdjustment;
import com.project.backend.features.admin.business.repository.EmployeeYearEndAdjustmentRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class YearEndAdjustmentService {
    private static final Set<String> STATUSES = Set.of("DRAFT", "CONFIRMED");
    private final EmployeeYearEndAdjustmentRepository repository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<YearEndAdjustmentResponse> findByYear(int taxYear) {
        validateYear(taxYear);
        return repository.findByTenantIdAndTaxYearAndDeletedAtIsNullOrderByEmployeeEmployeeCodeAsc(
                TenantContext.getTenantId(), taxYear).stream().map(this::toResponse).toList();
    }

    public YearEndAdjustmentResponse save(YearEndAdjustmentSaveRequest request) {
        validateYear(request.taxYear());
        if (!STATUSES.contains(request.status())) throw new IllegalArgumentException("状態が不正です。");
        String tenantId = TenantContext.getTenantId();
        Employee employee = employeeRepository.findByIdAndDeletedAtIsNull(request.employeeId())
                .orElseThrow(() -> new IllegalArgumentException("従業員が見つかりません。"));
        if (!tenantId.equals(employee.getTenantId())) {
            throw new IllegalArgumentException("従業員が見つかりません。");
        }
        EmployeeYearEndAdjustment entity;
        if (request.id() != null && request.id() > 0) {
            entity = repository.findByIdAndTenantIdAndDeletedAtIsNull(request.id(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("年末調整データが見つかりません。"));
            if (request.lockVersion() != null && !request.lockVersion().equals(entity.getLockVersion())) {
                throw new IllegalStateException("年末調整データが更新されています。再読み込みしてください。");
            }
        } else {
            entity = repository.findByTenantIdAndEmployeeIdAndTaxYearAndDeletedAtIsNull(
                    tenantId, request.employeeId(), request.taxYear())
                    .orElseGet(EmployeeYearEndAdjustment::new);
        }
        entity.setEmployee(employee);
        entity.setTaxYear(request.taxYear());
        entity.setTargetMonth(request.targetMonth().atDay(1));
        entity.setAdjustmentAmount(request.adjustmentAmount());
        entity.setStatus(request.status());
        entity.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        return toResponse(repository.save(entity));
    }

    public void delete(Long id) {
        EmployeeYearEndAdjustment entity = repository.findByIdAndTenantIdAndDeletedAtIsNull(
                id, TenantContext.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("年末調整データが見つかりません。"));
        entity.setDeletedAt(Instant.now(clock));
    }

    private void validateYear(int year) {
        if (year < 2000 || year > 2200) throw new IllegalArgumentException("対象年度が不正です。");
    }

    private YearEndAdjustmentResponse toResponse(EmployeeYearEndAdjustment entity) {
        return new YearEndAdjustmentResponse(entity.getId(), entity.getEmployee().getId(),
                entity.getEmployee().getEmployeeCode(), entity.getEmployee().getEmployeeName(),
                entity.getTaxYear(), java.time.YearMonth.from(entity.getTargetMonth()),
                entity.getAdjustmentAmount(), entity.getStatus(), entity.getNote(), entity.getLockVersion());
    }
}
