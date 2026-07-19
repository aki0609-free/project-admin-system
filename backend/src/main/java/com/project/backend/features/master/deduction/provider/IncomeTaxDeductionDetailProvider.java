package com.project.backend.features.master.deduction.provider;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.master.deduction.dto.BaseDeductionDetailResponse;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;
import com.project.backend.features.tax.mapper.DeductionTaxDetailMapper;
import com.project.backend.features.tax.repository.IncomeTaxBracketRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IncomeTaxDeductionDetailProvider implements DeductionDetailProvider {

    private final IncomeTaxBracketRepository incomeTaxBracketRepository;
    private final DeductionTaxDetailMapper mapper;

    @Override
    public DeductionDetailViewType supports() {
        return DeductionDetailViewType.INCOME_TAX;
    }

    @Override
    public List<BaseDeductionDetailResponse> getDetails(DeductionMaster deduction) {
        int currentYear = LocalDate.now().getYear();

        return incomeTaxBracketRepository
                .findByYearOrderByMinSalaryAscDependentsAsc(currentYear)
                .stream()
                .map(mapper::toIncomeTaxDetailResponse)
                .toList();
    }
}