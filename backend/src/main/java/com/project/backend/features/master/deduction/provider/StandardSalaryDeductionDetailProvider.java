package com.project.backend.features.master.deduction.provider;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.master.deduction.dto.BaseDeductionDetailResponse;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;
import com.project.backend.features.tax.mapper.DeductionTaxDetailMapper;
import com.project.backend.features.tax.repository.StandardSalaryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StandardSalaryDeductionDetailProvider implements DeductionDetailProvider {

    private final StandardSalaryRepository standardSalaryRepository;
    private final DeductionTaxDetailMapper mapper;

    @Override
    public DeductionDetailViewType supports() {
        return DeductionDetailViewType.PENSION;
    }

    @Override
    public List<BaseDeductionDetailResponse> getDetails(DeductionMaster deduction) {
        return standardSalaryRepository
                .findAllByOrderByMinSalaryAsc()
                .stream()
                .map(mapper::toStandardSalaryDetailResponse)
                .toList();
    }
}