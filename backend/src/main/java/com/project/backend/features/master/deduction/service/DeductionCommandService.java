package com.project.backend.features.master.deduction.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.features.master.deduction.dto.DeductionSaveRequest;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionCalculationType;
import com.project.backend.features.master.deduction.mapper.DeductionMapper;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeductionCommandService {

    private final DeductionMasterRepository deductionMasterRepository;
    private final DeductionMapper deductionMapper;

    @SuppressWarnings("null")
    public Long create(DeductionSaveRequest request) {
        validateForCreate(request);

        DeductionMaster entity = deductionMapper.toEntity(request);
        return deductionMasterRepository.save(entity).getId();
    }

    @SuppressWarnings("null")
    public void update(Long id, DeductionSaveRequest request) {
        @SuppressWarnings("null")
        DeductionMaster entity = deductionMasterRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("控除マスターが見つかりません。id=" + id)
                );

        validateForUpdate(id, request);

        deductionMapper.update(entity, request);
        deductionMasterRepository.save(entity);
    }

    @SuppressWarnings("null")
    public void delete(Long id) {
        DeductionMaster entity = deductionMasterRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("控除マスターが見つかりません。id=" + id)
                );

        deductionMasterRepository.delete(entity);
    }

    private void validateForCreate(DeductionSaveRequest request) {
        validateRequest(request);

        if (deductionMasterRepository.existsByDeductionCode(request.deductionCode())) {
            throw new IllegalArgumentException(
                    "同じ控除コードが既に存在します。deductionCode=" + request.deductionCode()
            );
        }
    }

    private void validateForUpdate(Long id, DeductionSaveRequest request) {
        validateRequest(request);

        deductionMasterRepository.findByDeductionCode(request.deductionCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "同じ控除コードが既に存在します。deductionCode=" + request.deductionCode()
                    );
                });
    }

    private void validateRequest(DeductionSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("リクエストが不正です。");
        }

        if (!StringUtils.hasText(request.deductionCode())) {
            throw new IllegalArgumentException("deductionCode は必須です。");
        }

        if (!StringUtils.hasText(request.deductionName())) {
            throw new IllegalArgumentException("deductionName は必須です。");
        }

        if (request.calculationType() == DeductionCalculationType.AUTO
                && !StringUtils.hasText(request.ruleName())) {
            throw new IllegalArgumentException("AUTO計算の場合 ruleName は必須です。");
        }
    }
}