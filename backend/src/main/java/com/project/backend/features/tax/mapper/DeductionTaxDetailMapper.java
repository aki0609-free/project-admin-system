package com.project.backend.features.tax.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.project.backend.features.master.deduction.dto.BaseDeductionDetailResponse;
import com.project.backend.features.tax.entity.IncomeTaxBracket;
import com.project.backend.features.tax.entity.InsuranceRate;
import com.project.backend.features.tax.entity.ResidentTaxMonthly;
import com.project.backend.features.tax.entity.StandardSalary;

@Component
public class DeductionTaxDetailMapper {

    public BaseDeductionDetailResponse toIncomeTaxDetailResponse(
            IncomeTaxBracket entity
    ) {
        Map<String, Object> values = new LinkedHashMap<>();

        values.put("year", entity.getYear());
        values.put("minSalary", entity.getMinSalary());
        values.put("maxSalary", entity.getMaxSalary());
        values.put("dependents", entity.getDependents());
        values.put("taxAmount", entity.getTaxAmount());

        return new BaseDeductionDetailResponse(
                entity.getId(),
                "INCOME_TAX",
                entity.getYear() + "年 所得税表",
                values
        );
    }

    public BaseDeductionDetailResponse toResidentTaxMonthlyDetailResponse(
            ResidentTaxMonthly entity
    ) {
        Map<String, Object> values = new LinkedHashMap<>();

        values.put("employeeId", entity.getEmployeeId());
        values.put("fiscalYear", entity.getFiscalYear());
        values.put("month", entity.getMonth());
        values.put("taxAmount", entity.getTaxAmount());

        return new BaseDeductionDetailResponse(
                entity.getId(),
                "RESIDENT_TAX",
                entity.getFiscalYear() + "年度 " + entity.getMonth() + "月 住民税",
                values
        );
    }

    public BaseDeductionDetailResponse toInsuranceRateDetailResponse(
            InsuranceRate entity
    ) {
        Map<String, Object> values = new LinkedHashMap<>();

        values.put("insuranceType", entity.getInsuranceType().name());
        values.put("year", entity.getYear());
        values.put("employeeRate", entity.getEmployeeRate());
        values.put("employerRate", entity.getEmployerRate());

        return new BaseDeductionDetailResponse(
                entity.getId(),
                entity.getInsuranceType().name(),
                entity.getYear() + "年 " + entity.getInsuranceType().name(),
                values
        );
    }

    public BaseDeductionDetailResponse toStandardSalaryDetailResponse(
            StandardSalary entity
    ) {
        Map<String, Object> values = new LinkedHashMap<>();

        values.put("minSalary", entity.getMinSalary());
        values.put("maxSalary", entity.getMaxSalary());
        values.put("standardSalary", entity.getStandardSalary());

        return new BaseDeductionDetailResponse(
                entity.getId(),
                "STANDARD_SALARY",
                "標準報酬月額 " + entity.getStandardSalary(),
                values
        );
    }
}