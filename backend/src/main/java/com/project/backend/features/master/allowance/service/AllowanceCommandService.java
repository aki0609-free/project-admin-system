package com.project.backend.features.master.allowance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.features.master.allowance.dto.AllowanceSaveRequest;
import com.project.backend.features.master.allowance.entity.AllowanceMaster;
import com.project.backend.features.master.allowance.enums.AllowanceCalculationType;
import com.project.backend.features.master.allowance.mapper.AllowanceMapper;
import com.project.backend.features.master.allowance.repository.AllowanceMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AllowanceCommandService {

    private final AllowanceMasterRepository allowanceMasterRepository;
    private final AllowanceMapper allowanceMapper;

    @SuppressWarnings("null")
    public Long create(AllowanceSaveRequest request) {
        validateForCreate(request);

        AllowanceMaster entity = allowanceMapper.toEntity(request);
        return allowanceMasterRepository.save(entity).getId();
    }

    @SuppressWarnings("null")
    public void update(Long id, AllowanceSaveRequest request) {
        AllowanceMaster entity = allowanceMasterRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("手当マスターが見つかりません。id=" + id)
                );

        validateForUpdate(id, request);

        allowanceMapper.update(entity, request);
        allowanceMasterRepository.save(entity);
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        @SuppressWarnings("null")
        AllowanceMaster entity = allowanceMasterRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("手当マスターが見つかりません。id=" + id)
                );

        allowanceMasterRepository.delete(entity);
    }

    private void validateForCreate(AllowanceSaveRequest request) {
        validateRequest(request);

        if (allowanceMasterRepository.existsByAllowanceCode(request.allowanceCode())) {
            throw new IllegalArgumentException(
                    "同じ手当コードが既に存在します。allowanceCode=" + request.allowanceCode()
            );
        }
    }

    private void validateForUpdate(Long id, AllowanceSaveRequest request) {
        validateRequest(request);

        allowanceMasterRepository.findByAllowanceCode(request.allowanceCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "同じ手当コードが既に存在します。allowanceCode=" + request.allowanceCode()
                    );
                });
    }

    private void validateRequest(AllowanceSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.allowanceCode())) {
            throw new IllegalArgumentException("allowanceCode は必須です。");
        }

        if (!StringUtils.hasText(request.allowanceName())) {
            throw new IllegalArgumentException("allowanceName は必須です。");
        }

        if (request.calculationType() == AllowanceCalculationType.AUTO
                && !StringUtils.hasText(request.ruleName())) {
            throw new IllegalArgumentException("AUTO計算の場合 ruleName は必須です。");
        }
    }
}