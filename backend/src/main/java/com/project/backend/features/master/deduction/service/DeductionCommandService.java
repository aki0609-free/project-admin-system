package com.project.backend.features.master.deduction.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.deduction.dto.DeductionSaveRequest;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.mapper.DeductionMapper;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.payrollitem.exception.PayrollItemMasterConflictException;
import com.project.backend.features.master.payrollitem.service.validation.PayrollItemMasterValidator;

import jakarta.persistence.EntityNotFoundException;
import com.project.backend.features.system.rule.enums.RuleType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeductionCommandService {

    private final DeductionMasterRepository repository;
    private final DeductionMapper mapper;
    private final PayrollItemMasterValidator validator;

    public Long create(DeductionSaveRequest request) {
        validateRequest(request);
        String tenantId = TenantContext.getTenantId();
        String code = validator.normalizeCode(request.deductionCode());

        if (repository.existsByTenantIdAndDeductionCodeAndDeletedAtIsNull(tenantId, code)) {
            throw new PayrollItemMasterConflictException(
                    "同じ控除コードが既に存在します。deductionCode=" + code
            );
        }

        DeductionMaster entity = mapper.toEntity(request);
        normalize(entity, request);
        entity.setTenantId(tenantId);
        return repository.save(entity).getId();
    }

    public void update(Long id, DeductionSaveRequest request) {
        String tenantId = TenantContext.getTenantId();
        DeductionMaster entity = repository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "控除マスターが見つかりません。id=" + id
                ));

        validateRequest(request);
        String code = validator.normalizeCode(request.deductionCode());
        if (!entity.getDeductionCode().equals(code)) {
            throw new PayrollItemMasterConflictException("控除コードは作成後に変更できません。");
        }

        mapper.update(entity, request);
        normalize(entity, request);
        repository.save(entity);
    }

    public void delete(Long id) {
        String tenantId = TenantContext.getTenantId();
        DeductionMaster entity = repository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "控除マスターが見つかりません。id=" + id
                ));

        entity.setEnabled(false);
        entity.setDeletedAt(Instant.now());
        repository.save(entity);
    }

    private void validateRequest(DeductionSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("リクエストが不正です。");
        }
        validator.validate(
                request.deductionCode(),
                request.deductionName(),
                request.calculationType() == null ? null : request.calculationType().name(),
                request.ruleName(),
                request.defaultAmount(),
                request.allowManualInput(),
                request.minAmount(),
                request.maxAmount(),
                request.displayOrder(),
                RuleType.DEDUCTION
        );
    }

    private void normalize(DeductionMaster entity, DeductionSaveRequest request) {
        String calculationType = request.calculationType().name();
        entity.setDeductionCode(validator.normalizeCode(request.deductionCode()));
        entity.setDeductionName(request.deductionName().trim());
        entity.setRuleName(validator.normalizeRuleName(calculationType, request.ruleName()));
    }
}
