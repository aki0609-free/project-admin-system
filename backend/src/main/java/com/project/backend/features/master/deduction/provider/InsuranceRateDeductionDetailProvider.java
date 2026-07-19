package com.project.backend.features.master.deduction.provider;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.master.deduction.dto.BaseDeductionDetailResponse;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;
import com.project.backend.features.tax.mapper.DeductionTaxDetailMapper;
import com.project.backend.features.tax.repository.InsuranceRateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InsuranceRateDeductionDetailProvider implements DeductionDetailProvider {

    private final InsuranceRateRepository insuranceRateRepository;
    private final DeductionTaxDetailMapper mapper;

    @Override
    public DeductionDetailViewType supports() {
        return DeductionDetailViewType.HEALTH_INSURANCE;
    }

    @Override
    public List<BaseDeductionDetailResponse> getDetails(DeductionMaster deduction) {
        int year = LocalDate.now().getYear();

        return insuranceRateRepository
                .findByYearOrderByInsuranceTypeAsc(year)
                .stream()
                .map(mapper::toInsuranceRateDetailResponse)
                .toList();
    }
}