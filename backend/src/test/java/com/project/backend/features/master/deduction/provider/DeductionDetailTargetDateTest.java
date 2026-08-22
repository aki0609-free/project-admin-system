package com.project.backend.features.master.deduction.provider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.tax.enums.InsuranceType;
import com.project.backend.features.tax.mapper.DeductionTaxDetailMapper;
import com.project.backend.features.tax.repository.IncomeTaxBracketRepository;
import com.project.backend.features.tax.repository.InsuranceRateRepository;
import com.project.backend.features.tax.repository.ResidentTaxMonthlyRepository;

class DeductionDetailTargetDateTest {

    private final DeductionMaster deduction = new DeductionMaster();
    private final DeductionTaxDetailMapper mapper = mock(DeductionTaxDetailMapper.class);

    @Test
    void incomeTax_shouldUseTargetDateYear() {
        IncomeTaxBracketRepository repository = mock(IncomeTaxBracketRepository.class);
        when(repository.findByYearOrderByMinSalaryAscDependentsAsc(2024))
                .thenReturn(List.of());

        new IncomeTaxDeductionDetailProvider(repository, mapper)
                .getDetails(deduction, LocalDate.of(2024, 12, 31));

        verify(repository).findByYearOrderByMinSalaryAscDependentsAsc(2024);
    }

    @Test
    void healthInsurance_shouldUseTargetDateYear() {
        InsuranceRateRepository repository = mock(InsuranceRateRepository.class);
        when(repository.findByInsuranceTypeAndYearOrderByIdAsc(
                InsuranceType.HEALTH_INSURANCE, 2025
        )).thenReturn(List.of());

        new InsuranceRateDeductionDetailProvider(repository, mapper)
                .getDetails(deduction, LocalDate.of(2025, 4, 1));

        verify(repository).findByInsuranceTypeAndYearOrderByIdAsc(
                InsuranceType.HEALTH_INSURANCE, 2025
        );
    }

    @Test
    void employmentInsurance_shouldUseTargetDateYear() {
        InsuranceRateRepository repository = mock(InsuranceRateRepository.class);
        when(repository.findByInsuranceTypeAndYearOrderByIdAsc(
                InsuranceType.EMPLOYMENT_INSURANCE, 2025
        )).thenReturn(List.of());

        new EmploymentInsuranceDeductionDetailProvider(repository, mapper)
                .getDetails(deduction, LocalDate.of(2025, 4, 1));

        verify(repository).findByInsuranceTypeAndYearOrderByIdAsc(
                InsuranceType.EMPLOYMENT_INSURANCE, 2025
        );
    }

    @Test
    void residentTax_shouldUsePreviousFiscalYearThroughMay() {
        ResidentTaxMonthlyRepository repository = mock(ResidentTaxMonthlyRepository.class);
        when(repository.findByFiscalYearOrderByEmployeeIdAscMonthAsc(2025))
                .thenReturn(List.of());

        new ResidentTaxDeductionDetailProvider(repository, mapper)
                .getDetails(deduction, LocalDate.of(2026, 5, 31));

        verify(repository).findByFiscalYearOrderByEmployeeIdAscMonthAsc(2025);
    }

    @Test
    void residentTax_shouldSwitchFiscalYearInJune() {
        ResidentTaxMonthlyRepository repository = mock(ResidentTaxMonthlyRepository.class);
        when(repository.findByFiscalYearOrderByEmployeeIdAscMonthAsc(2026))
                .thenReturn(List.of());

        new ResidentTaxDeductionDetailProvider(repository, mapper)
                .getDetails(deduction, LocalDate.of(2026, 6, 1));

        verify(repository).findByFiscalYearOrderByEmployeeIdAscMonthAsc(2026);
    }
}
