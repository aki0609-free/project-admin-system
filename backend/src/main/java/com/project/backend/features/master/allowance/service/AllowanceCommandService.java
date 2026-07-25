package com.project.backend.features.master.allowance.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.allowance.dto.AllowanceSaveRequest;
import com.project.backend.features.master.allowance.entity.AllowanceMaster;
import com.project.backend.features.master.allowance.mapper.AllowanceMapper;
import com.project.backend.features.master.allowance.repository.AllowanceMasterRepository;
import com.project.backend.features.master.payrollitem.exception.PayrollItemMasterConflictException;
import com.project.backend.features.master.payrollitem.service.validation.PayrollItemMasterValidator;

import jakarta.persistence.EntityNotFoundException;
import com.project.backend.features.system.rule.enums.RuleType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AllowanceCommandService {

    private final AllowanceMasterRepository repository;
    private final AllowanceMapper mapper;
    private final PayrollItemMasterValidator validator;

    public Long create(AllowanceSaveRequest request) {
        validateRequest(request);
        String tenantId = TenantContext.getTenantId();
        String code = validator.normalizeCode(request.allowanceCode());

        if (repository.existsByTenantIdAndAllowanceCodeAndDeletedAtIsNull(tenantId, code)) {
            throw new PayrollItemMasterConflictException(
                    "同じ手当コードが既に存在します。allowanceCode=" + code
            );
        }

        AllowanceMaster entity = mapper.toEntity(request);
        normalize(entity, request);
        entity.setTenantId(tenantId);
        return repository.save(entity).getId();
    }

    public void update(Long id, AllowanceSaveRequest request) {
        String tenantId = TenantContext.getTenantId();
        AllowanceMaster entity = repository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "手当マスターが見つかりません。id=" + id
                ));

        validateRequest(request);
        String code = validator.normalizeCode(request.allowanceCode());
        if (!entity.getAllowanceCode().equals(code)) {
            throw new PayrollItemMasterConflictException("手当コードは作成後に変更できません。");
        }

        mapper.update(entity, request);
        normalize(entity, request);
        repository.save(entity);
    }

    public void delete(Long id) {
        String tenantId = TenantContext.getTenantId();
        AllowanceMaster entity = repository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "手当マスターが見つかりません。id=" + id
                ));

        entity.setEnabled(false);
        entity.setDeletedAt(Instant.now());
        repository.save(entity);
    }

    private void validateRequest(AllowanceSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("リクエストが不正です。");
        }
        validator.validate(
                request.allowanceCode(),
                request.allowanceName(),
                request.calculationType() == null ? null : request.calculationType().name(),
                request.ruleName(),
                request.defaultAmount(),
                request.allowManualInput(),
                request.minAmount(),
                request.maxAmount(),
                request.displayOrder(),
                RuleType.ALLOWANCE
        );
    }

    private void normalize(AllowanceMaster entity, AllowanceSaveRequest request) {
        String calculationType = request.calculationType().name();
        entity.setAllowanceCode(validator.normalizeCode(request.allowanceCode()));
        entity.setAllowanceName(request.allowanceName().trim());
        entity.setRuleName(validator.normalizeRuleName(calculationType, request.ruleName()));
    }
}
