package com.project.backend.features.master.deduction.provider;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.master.deduction.dto.BaseDeductionDetailResponse;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;
import com.project.backend.features.tax.mapper.DeductionTaxDetailMapper;
import com.project.backend.features.tax.repository.ResidentTaxMonthlyRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResidentTaxDeductionDetailProvider implements DeductionDetailProvider {

    private final ResidentTaxMonthlyRepository residentTaxMonthlyRepository;
    private final DeductionTaxDetailMapper mapper;
    private final Clock clock;

    @Override
    public DeductionDetailViewType supports() {
        return DeductionDetailViewType.RESIDENT_TAX;
    }

    @Override
    public List<BaseDeductionDetailResponse> getDetails(DeductionMaster deduction) {
        LocalDate today = LocalDate.now(clock);
        int fiscalYear = today.getYear() - (today.getMonthValue() < 6 ? 1 : 0);

        return residentTaxMonthlyRepository
                .findByFiscalYearOrderByEmployeeIdAscMonthAsc(fiscalYear)
                .stream()
                .map(mapper::toResidentTaxMonthlyDetailResponse)
                .toList();
    }
}
