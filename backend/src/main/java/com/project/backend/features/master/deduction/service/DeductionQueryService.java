package com.project.backend.features.master.deduction.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.master.deduction.dto.DeductionDetailResponse;
import com.project.backend.features.master.deduction.dto.DeductionListItemResponse;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.mapper.DeductionMapper;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.deduction.service.resolver.DeductionDetailResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeductionQueryService {

    private final DeductionMasterRepository deductionMasterRepository;
    private final DeductionMapper deductionMapper;
    private final DeductionDetailResolver deductionDetailResolver;

    public List<DeductionListItemResponse> findAll() {
        return deductionMasterRepository.findAll().stream()
                .map(deductionMapper::toListItem)
                .toList();
    }

    @SuppressWarnings("null")
    public DeductionDetailResponse findDetail(Long id) {
        DeductionMaster deduction = deductionMasterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("控除マスターが見つかりません。id=" + id));

        return deductionMapper.toDetail(
                deduction,
                deductionDetailResolver.resolve(deduction)
        );
    }
}