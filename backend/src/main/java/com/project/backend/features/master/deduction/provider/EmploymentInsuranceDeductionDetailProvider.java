package com.project.backend.features.master.deduction.provider;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.master.deduction.dto.BaseDeductionDetailResponse;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;
import com.project.backend.features.tax.enums.InsuranceType;
import com.project.backend.features.tax.mapper.DeductionTaxDetailMapper;
import com.project.backend.features.tax.repository.InsuranceRateRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmploymentInsuranceDeductionDetailProvider implements DeductionDetailProvider {

    private final InsuranceRateRepository insuranceRateRepository;
    private final DeductionTaxDetailMapper mapper;
    private final Clock clock;

    @Override
    public DeductionDetailViewType supports() {
        return DeductionDetailViewType.EMPLOYMENT_INSURANCE;
    }

    @Override
    public List<BaseDeductionDetailResponse> getDetails(DeductionMaster deduction) {
        int year = clock.instant().atZone(clock.getZone()).getYear();

        return insuranceRateRepository
                .findByInsuranceTypeAndYearOrderByIdAsc(
                        InsuranceType.EMPLOYMENT_INSURANCE,
                        year
                )
                .stream()
                .map(mapper::toInsuranceRateDetailResponse)
                .toList();
    }
}
