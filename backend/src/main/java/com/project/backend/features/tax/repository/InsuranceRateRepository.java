package com.project.backend.features.tax.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.tax.entity.InsuranceRate;
import com.project.backend.features.tax.enums.InsuranceType;

public interface InsuranceRateRepository
        extends JpaRepository<InsuranceRate, Long> {

    List<InsuranceRate> findByYearOrderByInsuranceTypeAsc(Integer year);

    List<InsuranceRate> findByInsuranceTypeAndYearOrderByIdAsc(
            InsuranceType insuranceType,
            Integer year
    );
}