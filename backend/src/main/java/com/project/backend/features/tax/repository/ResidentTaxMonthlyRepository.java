package com.project.backend.features.tax.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.tax.entity.ResidentTaxMonthly;

public interface ResidentTaxMonthlyRepository
        extends JpaRepository<ResidentTaxMonthly, Long> {

    List<ResidentTaxMonthly> findByFiscalYearOrderByEmployeeIdAscMonthAsc(
            Integer fiscalYear
    );

    List<ResidentTaxMonthly> findByEmployeeIdAndFiscalYearOrderByMonthAsc(
            Long employeeId,
            Integer fiscalYear
    );

    Optional<ResidentTaxMonthly> findByEmployeeIdAndFiscalYearAndMonth(
            Long employeeId,
            Integer fiscalYear,
            Integer month
    );

    void deleteByFiscalYear(Integer fiscalYear);

    void deleteByEmployeeIdAndFiscalYear(Long employeeId, Integer fiscalYear);
}